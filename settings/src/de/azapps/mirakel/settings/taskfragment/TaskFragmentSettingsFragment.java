/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.settings.taskfragment;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import de.azapps.mirakel.adapter.DragNDropListView;
import de.azapps.mirakel.adapter.DragNDropListView.RemoveListener;
import de.azapps.mirakel.helper.MirakelViewPreferences;
import de.azapps.mirakel.settings.R;
import de.azapps.tools.Log;

@SuppressLint("NewApi")
public class TaskFragmentSettingsFragment extends Fragment {
	public static final int ADD_KEY=-1;
	private final static String TAG = "de.azapps.mirakel.settings.taskfragment.TaskFragmentSettings";
	protected TaskFragmentSettingsAdapter adapter;
	protected  DragNDropListView listView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activity_task_fragment_settings,
				null);
		setupView(view);
		getActivity().getActionBar().setTitle(R.string.settings_task_fragment);
		return view;
	}

	void setupView(View v) {
		final List<Integer> values = MirakelViewPreferences.getTaskFragmentLayout();
		values.add(ADD_KEY);

		if (this.adapter != null) {
			this.adapter.changeData(values);
			this.adapter.notifyDataSetChanged();
			return;
		}

		this.adapter = new TaskFragmentSettingsAdapter(getActivity(),
				R.layout.row_taskfragment_settings, values);
		this.listView = (DragNDropListView) v.findViewById(R.id.taskfragment_list);
		this.listView.setEnableDrag(true);
		this.listView.setItemsCanFocus(true);
		this.listView.setAdapter(this.adapter);
		this.listView.requestFocus();
		this.listView.setDragListener(new DragNDropListView.DragListener() {

			@Override
			public void onDrag(int x, int y, ListView l) {
				// Nothing
			}

			@Override
			public void onStartDrag(View itemView) {
				itemView.setVisibility(View.INVISIBLE);

			}

			@Override
			public void onStopDrag(View itemView) {
				itemView.setVisibility(View.VISIBLE);

			}
		});
		this.listView.setDropListener(new DragNDropListView.DropListener() {

			@Override
			public void onDrop(int from, int to) {
				if (from != to&&to!=TaskFragmentSettingsFragment.this.listView.getCount()-1) {
					TaskFragmentSettingsFragment.this.adapter.onDrop(from, to);
					TaskFragmentSettingsFragment.this.listView.requestLayout();
				}
				Log.e(TAG, "Drop from:" + from + " to:" + to);

			}
		});

		this.listView.setOnItemClickListener(null);
		this.listView.setRemoveListener(new RemoveListener() {

			@Override
			public void onRemove(int which) {
				if(which!=TaskFragmentSettingsFragment.this.adapter.getCount()-1) {
					TaskFragmentSettingsFragment.this.adapter.onRemove(which);
				}
			}
		});
		this.listView.allowRemove(true);
	}

}
