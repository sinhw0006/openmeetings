/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.web.room.sidebar;

import static org.apache.openmeetings.web.app.Application.getBean;
import static org.apache.openmeetings.web.app.WebSession.getUserId;

import java.util.Arrays;
import java.util.List;

import org.apache.openmeetings.core.data.file.FileProcessor;
import org.apache.openmeetings.db.dao.basic.ConfigurationDao;
import org.apache.openmeetings.db.entity.file.FileExplorerItem;
import org.apache.openmeetings.db.entity.file.FileItem;
import org.apache.openmeetings.util.StoredFile;
import org.apache.openmeetings.util.process.ConverterProcessResultList;
import org.apache.openmeetings.web.app.Application;
import org.apache.openmeetings.web.room.RoomPanel;
import org.apache.openmeetings.web.util.BootstrapFileUploadBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadProgressBar;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.Strings;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.widget.dialog.AbstractFormDialog;
import com.googlecode.wicket.jquery.ui.widget.dialog.DialogButton;
import com.googlecode.wicket.kendo.ui.panel.KendoFeedbackPanel;

public class UploadDialog extends AbstractFormDialog<String> {
	private static final long serialVersionUID = 1L;
	private final KendoFeedbackPanel feedback = new KendoFeedbackPanel("feedback", new Options("button", true));
	private final Form<String> form;
	private final DialogButton upload;
	private final DialogButton cancel;
	private final FileUploadField uploadField;
	private final HiddenField<String> fileName;
	private final RoomFilePanel roomFiles;
	private final RoomPanel room;

	public UploadDialog(String id, RoomPanel room, RoomFilePanel roomFiles) {
		super(id, Application.getString(304));
		this.roomFiles = roomFiles;
		this.room = room;
		add(form = new Form<>("form"));
		upload = new DialogButton("upload", Application.getString(593), false) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isIndicating() {
				return true;
			}
		};
		cancel = new DialogButton("close", Application.getString(85));
		form.add(feedback.setOutputMarkupId(true));
	
		form.setMultiPart(true);
		form.setMaxSize(Bytes.bytes(getBean(ConfigurationDao.class).getMaxUploadSize()));
		// Model is necessary here to avoid writing image to the User object
		form.add(uploadField = new FileUploadField("file", new IModel<List<FileUpload>>() {
			private static final long serialVersionUID = 1L;

			//FIXME this need to be eliminated
			@Override
			public void detach() {
			}
			
			@Override
			public void setObject(List<FileUpload> object) {
			}
			
			@Override
			public List<FileUpload> getObject() {
				return null;
			}
		}));
		uploadField.add(AttributeAppender.append("accept", StoredFile.getAcceptAttr()));
		Form<String> nameForm = new Form<>("name-form");
		fileName = new HiddenField<>("name", Model.of(""));
		fileName.add(new AjaxFormSubmitBehavior(nameForm, "change") {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target) {
				if (!Strings.isEmpty(getComponent().getDefaultModelObjectAsString())) {
					upload.setEnabled(true, target);
				}
			}
		}).setOutputMarkupId(true);
		form.add(new UploadProgressBar("progress", form, uploadField));
		add(nameForm.add(fileName.setOutputMarkupId(true)));
		add(BootstrapFileUploadBehavior.INSTANCE);
	}

	@Override
	public void onClick(AjaxRequestTarget target, DialogButton button) {
		if (button == null || button.match("close")) {
			super.onClick(target, button);
		}
	}
	
	@Override
	protected List<DialogButton> getButtons() {
		return Arrays.asList(upload, cancel);
	}
	
	@Override
	public DialogButton getSubmitButton() {
		return upload;
	}

	@Override
	public Form<?> getForm() {
		return form;
	}

	@Override
	protected void onOpen(IPartialPageRequestHandler handler) {
		super.onOpen(handler);
		handler.appendJavaScript(String.format("bindUpload('%s', '%s');", form.getMarkupId(), fileName.getMarkupId()));
	}
	
	@Override
	protected void onError(AjaxRequestTarget target) {
		target.add(feedback);
	}

	@Override
	protected void onSubmit(AjaxRequestTarget target) {
		FileUpload fu = uploadField.getFileUpload();
		if (fu != null) {
			FileExplorerItem f = new FileExplorerItem();
			f.setSize(fu.getSize());
			f.setName(fu.getClientFileName());
			FileItem parent = roomFiles.getSelected();
			if (parent == null || !(parent instanceof FileExplorerItem)) {
				f.setOwnerId(getUserId());
			} else {
				f.setRoomId(parent.getRoomId());
				f.setOwnerId(parent.getOwnerId());
				if (parent.getId() > 0) {
					f.setParentId(FileItem.Type.Folder == parent.getType() ? parent.getId() : parent.getParentId());
				}
			}
			f.setInsertedBy(getUserId());
			
			try {
				ConverterProcessResultList result = getBean(FileProcessor.class).processFile(getUserId(), f, fu.getInputStream());
				if (result.hasError()) {
					error(result.getLogMessage());
				} else {
					close(target, null);
				}
			} catch (Exception e) {
				error(e.getMessage());
			}
		}
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(new PriorityHeaderItem(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(UploadDialog.class, "upload.js"))));
	}
}
