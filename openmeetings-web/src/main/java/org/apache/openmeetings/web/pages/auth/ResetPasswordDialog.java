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
package org.apache.openmeetings.web.pages.auth;

import org.apache.openmeetings.core.util.StrongPasswordValidator;
import org.apache.openmeetings.db.dao.user.UserDao;
import org.apache.openmeetings.db.entity.user.User;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.BootstrapAjaxButton;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;

public class ResetPasswordDialog extends Modal<String> {
	private static final long serialVersionUID = 1L;
	private Form<String> form = new ResetForm("form");
	private final NotificationPanel feedback = new NotificationPanel("feedback");
	private PasswordTextField password;
	private final User user;
	private final Modal<String> resetInfo;

	@SpringBean
	private UserDao userDao;

	public ResetPasswordDialog(String id, final User user, Modal<String> resetInfo) {
		super(id);
		this.user = user;
		this.resetInfo = resetInfo;
	}

	@Override
	protected void onInitialize() {
		header(new ResourceModel("325"));
		setCloseOnEscapeKey(false);
		setBackdrop(Backdrop.STATIC);
		show(true);

		add(form);
		addButton(new BootstrapAjaxButton("button", new ResourceModel("327"), form, Buttons.Type.Primary) {
			private static final long serialVersionUID = 1L;
		}); // Reset

		super.onInitialize();
	}

	private class ResetForm extends Form<String> {
		private static final long serialVersionUID = 1L;
		private TextField<String> login;
		private PasswordTextField confirmPassword;

		private ResetForm(String id) {
			super(id);
		}

		@Override
		protected void onInitialize() {
			super.onInitialize();
			add(feedback.setOutputMarkupId(true));
			add(login = new TextField<>("login", Model.of(user.getLogin())));
			login.setOutputMarkupId(true);
			add(password = new PasswordTextField("password", new Model<String>()));
			password.setLabel(new ResourceModel("328")).setOutputMarkupId(true);
			password.setRequired(false).add(new StrongPasswordValidator(user));
			add(confirmPassword = new PasswordTextField("confirmPassword", new Model<String>()));
			confirmPassword.setLabel(new ResourceModel("116")).setOutputMarkupId(true);

			add(new AjaxButton("submit") { // FAKE button so "submit-on-enter" works as expected
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(AjaxRequestTarget target) {
					ResetForm.this.onSubmit(target);
				}

				@Override
				protected void onError(AjaxRequestTarget target) {
					ResetForm.this.onError(target);
				}
			});
		}

		@Override
		protected void onValidate() {
			String pass = password.getConvertedInput();
			if (pass != null && !pass.isEmpty() && !pass.equals(confirmPassword.getConvertedInput())) {
				error(getString("232"));
			}
			super.onValidate();
		}

		@Override
		protected void onError() {
			RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(this::onError);
		}

		protected void onError(AjaxRequestTarget target) {
			target.add(feedback);
		}

		@Override
		protected void onSubmit() {
			RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(this::onSubmit);
		}

		protected void onSubmit(AjaxRequestTarget target) {
			try {
				userDao.resetPassword(user, password.getConvertedInput());
				ResetPasswordDialog.this.close(target);
				resetInfo.show(target);
			} catch (Exception e) {
				error(e.getMessage());
			}
		}
	}
}
