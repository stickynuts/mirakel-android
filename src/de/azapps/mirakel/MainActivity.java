package de.azapps.mirakel;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

/**
 * @see https 
 *      ://thepseudocoder.wordpress.com/2011/10/13/android-tabs-viewpager-swipe
 *      -able-tabs-ftw/
 * @author az
 * 
 */
public class MainActivity extends FragmentActivity implements
		ViewPager.OnPageChangeListener {

	/**
	 * /** The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	private PagerAdapter mPagerAdapter;
	protected ListFragment listFragment;
	protected TasksFragment tasksFragment;
	protected TaskFragment taskFragment;
	private Menu menu;
	private TasksDataSource taskDataSource;
	private ListsDataSource listDataSource;
	private Task currentTask;
	private List_mirakle currentList;
	private int LIST_FRAGMENT = 0, TASKS_FRAGMENT = 1, TASK_FRAGMENT = 2;
	protected static final int RESULT_SPEECH_NAME = 1,
			RESULT_SPEECH_CONTENT = 2, RESULT_SPEECH = 3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		taskDataSource = new TasksDataSource(this);
		taskDataSource.open();
		listDataSource = new ListsDataSource(this);
		listDataSource.open();

		setCurrentList(listDataSource.getList(0));

		// Intialise ViewPager
		this.intialiseViewPager();
		mViewPager.setCurrentItem(TASKS_FRAGMENT);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu = menu;
		onPageSelected(TASKS_FRAGMENT);
		return true;
	}

	private List<List_mirakle> lists;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete:
			new AlertDialog.Builder(this)
					.setTitle(this.getString(R.string.task_delete_title))
					.setMessage(this.getString(R.string.task_delete_content))
					.setPositiveButton(this.getString(R.string.Yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									taskDataSource.deleteTask(currentTask);
									setCurrentList(currentList);
								}
							})
					.setNegativeButton(this.getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing
								}
							}).show();
			return true;
		case R.id.menu_move:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.dialog_move);
			ListsDataSource listds = new ListsDataSource(this);
			listds.open();
			lists = listds.getAllLists();
			listds.close();
			List<CharSequence> items = new ArrayList<CharSequence>();
			final List<Integer> list_ids = new ArrayList<Integer>();
			for (List_mirakle list : lists) {
				if (list.getId() > 0) {
					items.add(list.getName());
					list_ids.add(list.getId());
				}
			}

			builder.setItems(items.toArray(new CharSequence[items.size()]),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							currentTask.setListId(list_ids.get(item));
							taskDataSource.saveTask(currentTask);
							currentList=listDataSource.getList((int) currentTask.getListId());
							tasksFragment.update();
							listFragment.update();
						}
					});

			AlertDialog dialog = builder.create();
			dialog.show();
			return true;

		case R.id.list_delete:
			long listId = currentList.getId();
			if (listId == Mirakel.LIST_ALL || listId == Mirakel.LIST_DAILY
					|| listId == Mirakel.LIST_WEEKLY)
				return true;
			new AlertDialog.Builder(this)
					.setTitle(this.getString(R.string.list_delete_title))
					.setMessage(this.getString(R.string.list_delete_content))
					.setPositiveButton(this.getString(R.string.Yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									listDataSource.deleteList(currentList);
									currentList = listDataSource
											.getList(Mirakel.LIST_ALL);
									setCurrentList(currentList);
								}
							})
					.setNegativeButton(this.getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing
								}
							}).show();
			listFragment.update();
			return true;
		case R.id.task_sorting:
			final CharSequence[] SortingItems = getResources().getStringArray(
					R.array.task_sorting_items);
			AlertDialog.Builder SortingDialogBuilder = new AlertDialog.Builder(this);
			SortingDialogBuilder.setTitle(this.getString(R.string.task_sorting_title));
			SortingDialogBuilder.setItems(SortingItems, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					switch (item) {
					case 0:
						currentList.setSortBy(Mirakel.SORT_BY_OPT);
						break;
					case 1:
						currentList.setSortBy(Mirakel.SORT_BY_DUE);
						break;
					case 2:
						currentList.setSortBy(Mirakel.SORT_BY_PRIO);
						break;
					default:
						currentList.setSortBy(Mirakel.SORT_BY_ID);
						break;
					}
					listDataSource.saveList(currentList);
					tasksFragment.update();
					listFragment.update();
				}
			});
			AlertDialog alert = SortingDialogBuilder.create();
			alert.show();
			return true;
		case R.id.menu_new_list:
			listDataSource.createList(this.getString(R.string.list_menu_new_list));
			listFragment.update();
			return true;
		default:
			return super.onOptionsItemSelected(item);

		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onSaveInstanceState(android.os.Bundle)
	 */
	protected void onSaveInstanceState(Bundle outState) {
		// outState.putString("tab", mTabHost.getCurrentTabTag()); // save the
		// tab
		// selected
		super.onSaveInstanceState(outState);
	}

	/**
	 * Initialise ViewPager
	 */
	private void intialiseViewPager() {

		List<Fragment> fragments = new Vector<Fragment>();
		listFragment = new ListFragment();
		listFragment.setActivity(this);
		fragments.add(listFragment);
		tasksFragment = new TasksFragment();
		tasksFragment.setActivity(this);
		fragments.add(tasksFragment);
		taskFragment = new TaskFragment();
		taskFragment.setActivity(this);
		fragments.add(taskFragment);
		this.mPagerAdapter = new PagerAdapter(
				super.getSupportFragmentManager(), fragments);
		//
		this.mViewPager = (ViewPager) super.findViewById(R.id.viewpager);
		this.mViewPager.setAdapter(this.mPagerAdapter);
		this.mViewPager.setOnPageChangeListener(this);
		mViewPager.setOffscreenPageLimit(2);

	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		if (menu == null)
			return;
		int newmenu;
		switch (position) {
		case 0:
			newmenu = R.menu.activity_list;
			break;
		case 1:
			newmenu = R.menu.tasks;
			break;
		case 2:
			newmenu = R.menu.activity_task;
			break;
		default:
			Toast.makeText(getApplicationContext(), "Where are the dragons?",
					Toast.LENGTH_LONG).show();
			return;
		}

		// Configure to use the desired menu

		menu.clear();
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(newmenu, menu);
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	Task getCurrentTask() {
		return currentTask;
	}

	void setCurrentTask(Task currentTask) {
		this.currentTask = currentTask;
		if (taskFragment != null) {
			taskFragment.update();
			mViewPager.setCurrentItem(TASK_FRAGMENT);
		}
	}

	List_mirakle getCurrentList() {
		return currentList;
	}

	void setCurrentList(List_mirakle currentList) {
		this.currentList = currentList;
		if (tasksFragment != null) {
			tasksFragment.update();
			mViewPager.setCurrentItem(TASKS_FRAGMENT);
		}

		List<Task> currentTasks = taskDataSource.getTasks(currentList,
				currentList.getSortBy());
		if (currentTasks.size() == 0) {
			currentTask = new Task(getApplicationContext());
		} else {
			currentTask = currentTasks.get(0);
		}
		if (taskFragment != null) {
			taskFragment.update();
		}

	}

	public TasksDataSource getTaskDataSource() {
		return taskDataSource;
	}

	public ListsDataSource getListDataSource() {
		return listDataSource;
	}

	/**
	 * Ugly Wrapper TODO make it more beautiful
	 * 
	 * @param task
	 */
	void saveTask(Task task) {
		Log.v("Foo", "Saving task… (task:" + task.getId() + " – current:"
				+ currentTask.getId());
		taskDataSource.saveTask(task);
		if (task.getId() == currentTask.getId()) {
			currentTask = task;
			taskFragment.update();
		}
		tasksFragment.update();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && null != data) {
			ArrayList<String> text = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			switch (requestCode) {
			case RESULT_SPEECH_CONTENT:
				((EditText) findViewById(R.id.edit_content)).setText(text
						.get(0));
				break;
			case RESULT_SPEECH_NAME:
				((EditText) findViewById(R.id.edit_name)).setText(text.get(0));
				break;
			case RESULT_SPEECH:
				if (resultCode == RESULT_OK && null != data) {
					((EditText) tasksFragment.view.findViewById(R.id.tasks_new))
							.setText(text.get(0));
				}
				break;
			}
		}
	}

}