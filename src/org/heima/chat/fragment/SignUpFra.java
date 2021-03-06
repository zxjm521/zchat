package org.heima.chat.fragment;

import org.heima.chat.R;
import org.heima.chat.activity.LoginActivity;
import org.heima.chat.base.BaseFragment;
import org.heima.chat.db.AccountDao;
import org.heima.chat.db.FriendDao;
import org.heima.chat.db.MessageDao;
import org.heima.chat.domain.Account;
import org.heima.chat.domain.Friend;
import org.heima.chat.domain.Message;
import org.heima.chat.service.ChatCoreService;
import org.heima.chat.utils.CommonUtil;
import org.heima.chat.utils.ToastUtil;
import org.heima.chat.widget.DialogLoading;
import org.heima.lib.HMChatManager;
import org.heima.lib.HMError;
import org.heima.lib.callback.HMObjectCallBack;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
/**
 * 注册界面   Fragment
 * @author ZXJM
 * @date 2016年9月25日
 *
 */
public class SignUpFra extends BaseFragment implements OnClickListener {
	private String TAG = "SignUpFra";

	private EditText etAccount;
	private EditText etPwd;
	private Button btnSignUp;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fra_sign_up, container, false);

		initView(view);
		initEvent();
		return view;
	}

	private void initEvent() {
		btnSignUp.setOnClickListener(this);
	}

	private void initView(View view) {
		etAccount = (EditText) view.findViewById(R.id.et_sign_up_account);
		etPwd = (EditText) view.findViewById(R.id.et_sign_up_pwd);
		btnSignUp = (Button) view.findViewById(R.id.btn_sign_up);

	}

	@Override
	public void onClick(View v) {
		if (v == btnSignUp) {
			doSignUp();
		}
	}

	private void doSignUp() {
		Context context = getActivity();
		if (context == null) {
			return;
		}

		String account = etAccount.getText().toString().trim();
		if (TextUtils.isEmpty(account)) {
			ToastUtil.show(context, "用户名为空");
			return;
		}
		String password = etPwd.getText().toString().trim();
		if (TextUtils.isEmpty(password)) {
			ToastUtil.show(context, "密码为空");
			return;
		}

		final DialogLoading dialog = new DialogLoading(getActivity());
		dialog.show();

		HMChatManager.getInstance().register(account, password,
				new HMObjectCallBack<Account>() {

					@Override
					public void onSuccess(Account account) {
						Log.d(TAG, "注册成功!!!");
						dialog.dismiss();

						// 初始化用户连接安全信息
						HMChatManager.getInstance().initAccount(
								account.getAccount(), account.getToken());

						// 存储用户
						AccountDao dao = new AccountDao(getActivity());
						account.setCurrent(true);

						Account localAccount = dao.getByAccount(account
								.getAccount());
						if (localAccount != null) {
							dao.updateAccount(account);
						} else {
							dao.addAccount(account);
						}

						// 开启服务
						if (!CommonUtil.isServiceRunning(getActivity(),
								ChatCoreService.class)) {
							getActivity().startService(
									new Intent(getActivity(),
											ChatCoreService.class));
						}

						FriendDao friendDao = new FriendDao(getActivity());
						Friend friend = friendDao.queryFriendByAccount(
								account.getAccount(), "ZXChat");
						if (friend == null) {
							// 初始化通讯录
							friend = new Friend();
							friend.setOwner(account.getAccount());
							friend.setAccount("ZXChat");
							friend.setAlpha("H");
							friend.setArea("");
							friend.setIcon("");
							friend.setName("黑信团队");
							friend.setNickName("");
							friend.setSort(1000);

							friendDao.addFriend(friend);

							MessageDao messageDao = new MessageDao(
									getActivity());
							Message message = new Message();
							message.setAccount("ZXChat");
							message.setContent("欢迎使用黑信，黑信会给你带来更多精彩");
							message.setCreateTime(System.currentTimeMillis());
							message.setDirection(1);
							message.setOwner(account.getAccount());
							message.setRead(false);
							messageDao.addMessage(message);
						}

						((LoginActivity) getActivity()).go2FillInfo();
					}

					@Override
					public void onError(int error, String msg) {
						dialog.dismiss();

						switch (error) {
						case HMError.ERROR_CLIENT_NET:
							Log.d(TAG, "客户端网络异常");
							ToastUtil.show(getActivity(), "客户端网络异常");
							break;
						case HMError.ERROR_SERVER:
							Log.d(TAG, "服务器异常");
							ToastUtil.show(getActivity(), "服务器异常");
							break;
						case HMError.Register.ACCOUNT_EXIST:
							Log.d(TAG, "用户已经存在");
							ToastUtil.show(getActivity(), "用户已经存在");
							break;
						default:
							break;
						}
					}
				});
	}
}