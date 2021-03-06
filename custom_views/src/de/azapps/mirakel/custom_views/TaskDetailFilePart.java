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
package de.azapps.mirakel.custom_views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.tools.Log;

public class TaskDetailFilePart extends TaskDetailSubListBase<FileMirakel> {

	public interface OnFileClickListner{
		abstract public void clickOnFile(FileMirakel f);
	}

	public interface OnFileMarkedListner {
		abstract public void markFile(View v, FileMirakel e, boolean marked);
	}

	private static final String	TAG	= "TaskDetailFilePart";

	protected OnFileClickListner clickListner;
	private final Context	ctx;
	private FileMirakel	file;

	private final ImageView	fileImage;
	private final TextView	fileName;
	private final TextView	filePath;
	private boolean			marked;
	private OnFileMarkedListner	markedListner;

	public TaskDetailFilePart(Context context) {
		super(context);
		this.ctx=context;
		inflate(context, R.layout.files_row, this);
		this.fileImage = (ImageView) findViewById(R.id.file_image);
		this.fileName = (TextView) findViewById(R.id.file_name);
		this.filePath = (TextView) findViewById(R.id.file_path);
		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (TaskDetailFilePart.this.markedEnabled) {
					handleMark();
				} else if (TaskDetailFilePart.this.clickListner != null) {
					TaskDetailFilePart.this.clickListner
					.clickOnFile(TaskDetailFilePart.this.file);
				}
			}
		});
		setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				handleMark();
				return true;
			}
		});
	}

	private void handleMark() {
		if (this.markedListner != null) {
			TaskDetailFilePart.this.marked = !TaskDetailFilePart.this.marked;
			this.markedListner.markFile(this, TaskDetailFilePart.this.file,
					TaskDetailFilePart.this.marked);
		}
	}

	public void setOnFileClickListner(final OnFileClickListner l) {
		this.clickListner = l;
	}

	public void setOnFileMarked(final OnFileMarkedListner l) {
		this.markedListner = l;
	}


	public void setShortMark(boolean shortMark) {
		this.markedEnabled=shortMark;
	}

	@Override
	public void updatePart(FileMirakel f) {
		setBackgroundColor(this.context.getResources().getColor(
				android.R.color.transparent));
		// this will break the preview images...
		// if (f==null&&this.file==null||f!=null&&f.equals(this.file)) return;
		Log.d(TAG, "update");
		this.file = f;
		new Thread(new Runnable() {
			private Bitmap	preview;

			@Override
			public void run() {
				if (TaskDetailFilePart.this.file.getPath().endsWith(".mp3")) {
					int resource_id = MirakelCommonPreferences.isDark() ? R.drawable.ic_action_play_dark
							: R.drawable.ic_action_play;
					this.preview = BitmapFactory.decodeResource(
							TaskDetailFilePart.this.ctx.getResources(),
							resource_id);
				} else {
					this.preview = TaskDetailFilePart.this.file.getPreview();
				}
				if (this.preview != null) {
					Log.i(TAG, "preview not null");
					TaskDetailFilePart.this.fileImage.post(new Runnable() {
						@Override
						public void run() {
							TaskDetailFilePart.this.fileImage
							.setImageBitmap(preview);
							LayoutParams params = (LayoutParams) TaskDetailFilePart.this.fileImage
									.getLayoutParams();
							params.height = preview.getHeight();
							TaskDetailFilePart.this.fileImage
							.setLayoutParams(params);
						}
					});
				} else {
					Log.i(TAG, "preview null");
					TaskDetailFilePart.this.fileImage.post(new Runnable() {
						@Override
						public void run() {
							LayoutParams params = (LayoutParams) TaskDetailFilePart.this.fileImage
									.getLayoutParams();
							params.height = 0;
							TaskDetailFilePart.this.fileImage
							.setLayoutParams(params);

						}
					});
				}
			}
		}).start();
		if (this.file.getPath().endsWith(".mp3")
				&& this.file.getName().startsWith("AUD_")) {
			this.fileName.setText(R.string.audio_record_file);
		} else if (this.file.getName().endsWith(".jpg")) {
			this.fileName.setText(R.string.image_file);
		} else {
			this.fileName.setText(this.file.getName());
		}
		this.filePath.setText(this.file.getPath());
		if (!this.file.getFile().exists()) {
			this.filePath.setText(R.string.file_vanished);
		} else {
			this.filePath.setText(this.file.getPath());
		}
	}

}
