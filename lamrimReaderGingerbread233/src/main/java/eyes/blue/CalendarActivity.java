package eyes.blue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

//import net.simonvt.calendarview.CalendarView;
import com.csvreader.CsvReader;
import com.disegnator.robotocalendar.RobotoCalendarView;
import com.disegnator.robotocalendar.RobotoCalendarView.RobotoCalendarListener;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class CalendarActivity extends AppCompatActivity {
	Hashtable<String, GlRecord> glSchedule = new Hashtable<String, GlRecord>();
	ProgressDialog downloadPDialog = null;
	SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
	Date glRangeStart = null, glRangeEnd = null;
	FileSysManager fsm=null;
	View actionBarControlPanel = null;
	
	// For Calendar view.
	private RobotoCalendarView robotoCalendarView;
	private Calendar currentCalendar;
	private int currentMonthIndex;

	GlRecord selectedGlr = null;
	boolean dialogShowing = false;
	String selectedDay = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calendar);

		LayoutInflater factory = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		actionBarControlPanel = factory.inflate(R.layout.calendar_actionbar_control_panel, null);
		getSupportActionBar();
		getSupportActionBar().setCustomView(actionBarControlPanel);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
		initActionBarItem();

		initialCalendarView();
		downloadPDialog = new ProgressDialog(CalendarActivity.this);
		downloadPDialog.setTitle(getString(R.string.dlgTitleDownloading));
		downloadPDialog.setMessage(String.format(
				getString(R.string.dlgDescDownloading), "",
				getString(R.string.title_activity_calendar)));
		fsm=new FileSysManager(CalendarActivity.this);
	}

	private void initActionBarItem(){
		LinearLayout reloadStateOpt=(LinearLayout) actionBarControlPanel.findViewById(R.id.reloadStateOpt);
		LinearLayout downloadSchOpt=(LinearLayout) actionBarControlPanel.findViewById(R.id.downloadSchOpt);

		// Check is there not exist last state
		SharedPreferences playRecord = getSharedPreferences(getString(R.string.GLModeRecordFile), 0);
		String title=playRecord.getString("title", null);
		int mediaIndex=playRecord.getInt("mediaIndex",-1);
		int position=playRecord.getInt("playPosition", -1);
		if(title == null || mediaIndex == -1 || position == -1){
			reloadStateOpt.setVisibility(View.GONE);;
			//reloadStateOpt.findViewById(R.id.image).setVisibility(View.GONE);
			//reloadStateOpt.findViewById(R.id.desc).setVisibility(View.GONE);
		}

		reloadStateOpt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra("reloadLastState", true);
				setResult(Activity.RESULT_OK, intent);
				GaLogger.sendEvent("ui_action", "CalendarActivity", "ReloadLastState", null);
				finish();
			}
		});

		downloadSchOpt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(getClass().getName(),"dialogShowing = "+dialogShowing);
				if (dialogShowing)return;

				dialogShowing = true;
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (!downloadSchedule())
							return;

						File schedule = getLocalScheduleFile();
						if (schedule == null)
							return;
						reloadSchedule(schedule);
					}
				}).start();
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();

		new Thread(new Runnable() {
			@Override
			public void run() {
				File schedule = getLocalScheduleFile();
				if (schedule != null) {
					if (reloadSchedule(schedule))
						if (glRangeEnd.getTime() > System.currentTimeMillis())
							return;
				}

				dialogShowing = true;
				if (!downloadSchedule())
					return;
				schedule = getLocalScheduleFile();
				if (schedule == null)
					return;
				reloadSchedule(schedule);
			}
		}).start();
	}
/*
	private void adjustViewBottun(){
		Rect rectgle = new Rect();
		Window window = getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
		int StatusBarHeight = rectgle.top;
		int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
		int titleBarHeight = contentViewTop - StatusBarHeight;
		int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
		int contentViewHeight = screenHeight - contentViewTop;
		
		ScrollView sv=(ScrollView) findViewById(R.id.calendarRootView);
		if(sv.getHeight()<contentViewHeight)
			sv.setMinimumHeight(contentViewHeight);
	}
	*/
	@Override
	protected void onStop() {
		super.onStop();
	}
/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();// clear items
		SharedPreferences playRecord = getSharedPreferences(getString(R.string.GLModeRecordFile), 0);
		String title=playRecord.getString("title", null);
		int mediaIndex=playRecord.getInt("mediaIndex",-1);
		int position=playRecord.getInt("playPosition", -1);
		if(title != null && mediaIndex != -1 && position != -1){
			MenuItem item=menu.add(0,0,0,getString(R.string.reloadLastState) + ": " + title );
			item.setIcon(R.drawable.reload_last_state);
			MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			return super.onCreateOptionsMenu(menu);
		}

		MenuItem item1=menu.add(1,0,0,getString(R.string.downloadSchedule));
			item1.setIcon(R.drawable.update);
			MenuItemCompat.setShowAsAction(item1, MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String menuStr=item.getTitle().toString();
		if(menuStr.startsWith(getString(R.string.downloadSchedule))){
			Log.d(getClass().getName(),"dialogShowing = "+dialogShowing);
			if (dialogShowing)return true;
			dialogShowing = true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (!downloadSchedule())
						return;

					File schedule = getLocalScheduleFile();
					if (schedule == null)
						return;
					reloadSchedule(schedule);
				}
			}).start();
			return true;
		}
		else if(menuStr.startsWith(getString(R.string.reloadLastState))){
			Intent intent = new Intent();
			intent.putExtra("reloadLastState", true);
			setResult(Activity.RESULT_OK, intent);
			GaLogger.sendEvent("ui_action", "CalendarActivity", "ReloadLastState", null);
			finish();
			return true;
		}
		return false;
	}
*/
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		//Log.d("CalendarActivity","Get result from SpeechMenuActivity,
		Log.d("CalendarActivity","Get result from SpeechMenuActivity, Result="+((resultCode==RESULT_OK)?"OK":"Not OK")+", SelectedGlr="+selectedGlr/*+", isFileExist="+((isFileExist(selectedGlr))?"OK":"Not OK")*/);
		if (resultCode != RESULT_OK || selectedGlr == null || !isFileExist(selectedGlr))
			return;

		setResult(Activity.RESULT_OK, getResultIntent(selectedGlr));
		finish();
	}

	private void initialCalendarView() {
		robotoCalendarView = (RobotoCalendarView) findViewById(R.id.robotoCalendarPicker);

		// Initialize the RobotoCalendarPicker with the current index and date
		currentMonthIndex = 0;
		currentCalendar = Calendar.getInstance(Locale.getDefault());
		robotoCalendarView.initializeCalendar(currentCalendar);

		// Mark current day
		robotoCalendarView.markDayAsCurrentDay(currentCalendar.getTime());

		robotoCalendarView
				.setRobotoCalendarListener(new RobotoCalendarListener() {

					@Override
					public void onDateSelected(Date date) {
						if (dialogShowing)
							return;
						dialogShowing = true;

						robotoCalendarView.markDayAsSelectedDay(date);
						String key = dateFormater.format(date);
						selectedDay = key;
						final GlRecord glr = glSchedule.get(key);
						if (glr == null) {
							Log.d(getClass().getName(), "No record for: " + key);
							dialogShowing = false;
							return;
						}

						selectedGlr = glr;

						String msg = "日期: " + glr.dateStart + " ~ "
								+ glr.dateEnd + "\n";
						msg += "音檔: " + glr.speechPositionStart + " ~ "
								+ glr.speechPositionEnd + "\n";
						msg += "長度: " + glr.totalTime + "\n";
						msg += "廣論: " + glr.theoryLineStart + " ~ "
								+ glr.theoryLineEnd + "\n";
						msg += "手抄: " + glr.subtitleLineStart + " ~ "
								+ glr.subtitleLineEnd + "\n";
						msg += "內容: " + glr.desc;

						AlertDialog.Builder builder = new AlertDialog.Builder(CalendarActivity.this);
						builder.setTitle(key);
						builder.setMessage(msg);
						builder.setPositiveButton(getString(R.string.dlgOk),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										if (isFileExist(glr)) {
											Intent intent = getResultIntent(glr);
											setResult(Activity.RESULT_OK, intent);
											try {
												dialog.dismiss();
											} catch (Exception e) {
												e.printStackTrace();
											}    // Don't force close if problem here.
											dialogShowing = false;
											GaLogger.sendEvent("ui_action", "CalendarActivity", "ShowInfoDialog_" + intent.getStringExtra("selectedDay"), null);
											finish();
										} else {
											final Intent speechMenu = new Intent(CalendarActivity.this, SpeechMenuActivity.class);
											int[] speechStart = GlRecord.getSpeechStrToInt(glr.speechPositionStart);// {speechIndex,min,sec}
											int[] speechEnd = GlRecord.getSpeechStrToInt(glr.speechPositionEnd);// {speechIndex,min,sec}
											try {
												dialog.dismiss();
											} catch (Exception e) {
												e.printStackTrace();
											}    // Don't force close if problem here.
											dialogShowing = false;
											int[] intentCmd = null;
											if (speechStart[0] == speechEnd[0])
												intentCmd = new int[]{speechStart[0]};
											else
												intentCmd = fsm.getUnreadyList(speechStart[0], speechEnd[0]);

											speechMenu.putExtra("index", intentCmd);
											startActivityForResult(speechMenu, 0);
										}
									}
								});
						builder.setNegativeButton(
								getString(R.string.dlgCancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
														int id) {
										dialog.cancel();
										dialogShowing = false;
									}
								});
						builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								dialogShowing = false;
							}
						});
						builder.create().show();
					}

					@Override
					public void onRightButtonClick() {
						currentMonthIndex++;
						updateCalendar();
						if (currentMonthIndex == 0)
							robotoCalendarView
									.markDayAsCurrentDay(currentCalendar
											.getTime());
						markScheduleDays();
					}

					@Override
					public void onLeftButtonClick() {
						currentMonthIndex--;
						updateCalendar();
						if (currentMonthIndex == 0)
							robotoCalendarView
									.markDayAsCurrentDay(currentCalendar
											.getTime());
						markScheduleDays();
					}

					private void updateCalendar() {
						currentCalendar = Calendar.getInstance(Locale
								.getDefault());
						currentCalendar.add(Calendar.MONTH, currentMonthIndex);
						robotoCalendarView.initializeCalendar(currentCalendar);
					}
				});
	}

	/*
	 * Call this for update UI, it seems fire UI update too quick in short time,
	 * If I just place [robotoCalendarView.markDayWithStyle(style[index],
	 * month.getTime());] in runOnUiThread() the UI refresh fragmentation.
	 */
	private void markScheduleDays() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				markScheduleDaysInCalendar();
			}
		});
	}

	private void markScheduleDaysInCalendar() {
		Log.d(getClass().getName(), "Into Mark schedule function");
		final Calendar month = (Calendar) currentCalendar.clone();
		int daysOfMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
		Log.d(getClass().getName(), "Get Month: " + month.get(Calendar.YEAR)
				+ "/" + month.get(Calendar.MONTH) + ", there are "
				+ daysOfMonth);

		// final int style[]={RobotoCalendarView.BLUE_CIRCLE,
		// RobotoCalendarView.GREEN_CIRCLE, RobotoCalendarView.RED_CIRCLE};
		final int style[] = { RobotoCalendarView.BLUE_LINE,
				RobotoCalendarView.GREEN_LINE, RobotoCalendarView.RED_LINE };
		int styleIndex = 0;
		GlRecord lastGlr = null;
		for (int i = 1; i <= daysOfMonth; i++) {
			month.set(Calendar.DAY_OF_MONTH, i);
			String key = dateFormater.format(month.getTime());
			Log.d(getClass().getName(), "Get Data with key: " + key);
			GlRecord glr = glSchedule.get(key);
			if (glr == null)
				continue;

			if (lastGlr != null && glr != lastGlr)
				if (++styleIndex == style.length)
					styleIndex = 0;

			Log.d(getClass().getName(), "Mark " + month.get(Calendar.YEAR)
					+ "/" + month.get(Calendar.MONTH) + "/" + i + " as style"
					+ styleIndex);
			final int index = styleIndex;
			robotoCalendarView.markDayWithStyle(style[index], month.getTime());
			lastGlr = glr;
		}
	}

	private boolean isFileExist(GlRecord glr) {
		int[] speechStart = GlRecord.getSpeechStrToInt(glr.speechPositionStart);// {speechIndex, TimeMs}
		int[] speechEnd = GlRecord.getSpeechStrToInt(glr.speechPositionEnd);//  {speechIndex, TimeMs}

		File mediaStart = fsm.getLocalMediaFile(speechStart[0]);
		File subtitleStart = fsm.getLocalSubtitleFile(speechStart[0]);
		File mediaEnd = fsm.getLocalMediaFile(speechEnd[0]);
		File subtitleEnd = fsm.getLocalSubtitleFile(speechEnd[0]);
		
		if(mediaStart == null || subtitleStart == null || mediaEnd == null || subtitleEnd == null)
			return false;
		
		return (mediaStart.exists() && subtitleStart.exists() && mediaEnd.exists() && subtitleEnd.exists());
	}

	private File getLocalScheduleFile() {
		String scheFileName = getString(R.string.globalLamrimScheduleFile);
		String format = getString(R.string.globalLamrimScheduleFileFormat);
		File file = null;
		file = new File(getFilesDir() + File.separator + scheFileName + "."
				+ format);
		Log.d(getClass().getName(), "Schedule file: " + file.getAbsolutePath());
		if (file.exists())
			return file;
		return null;
	}

	private boolean reloadSchedule(File file) {
		CsvReader csvr = null;

		// Load the date range of file.
		try {
			csvr = new CsvReader(file.getAbsolutePath(), ',',
					Charset.forName("UTF-8"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView), getString(R.string.localGlobalLamrimScheduleFileNotFound));
			GaLogger.sendException("File: " + file.getAbsolutePath(), e, true);
			return false;
		}

		try {
			if (!csvr.readRecord()) {
				Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView), getString(R.string.localGlobalLamrimScheduleFileReadErr));
				GaLogger.sendException(
						"Error happen while csv reader read record csvr.readRecord()",
						new Exception("Error happen while read record of csv reader."),
						true);
				return false;
			}
			int count = csvr.getColumnCount();
			if (count < 2) {
				Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView), getString(R.string.localGlobalLamrimScheduleFileRangeFmtErr));
				GaLogger.sendException(
						"Error: the date start and date end colume of global lamrim schedule file is not 2 colume",
						new Exception("Global Lamrim schedule file format error."),	true);
				return false;
			}
			// DateFormat df = DateFormat.getDateInstance();
			// DateFormat df = new SimpleDateFormat("EE-MM-dd-yyyy");

			try {
				Date arg1 = dateFormater.parse(csvr.get(0));
				Date arg2 = dateFormater.parse(csvr.get(1));
				// Date arg1=df.parse(csvr.get(0));
				// Date arg2=df.parse(csvr.get(1));
				if (arg1.getTime() < arg2.getTime()) {
					glRangeStart = arg1;
					glRangeEnd = arg2;
				} else {
					glRangeStart = arg2;
					glRangeEnd = arg1;
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView), getString(R.string.localGlobalLamrimScheduleFileDateFmtErr));
				GaLogger.sendException(
						"Error happen while parse data region of Global Lamrim schedule file: data1="
								+ csvr.get(0) + ", data2=" + csvr.get(1), e,
						true);
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView), getString(R.string.localGlobalLamrimScheduleFileReadErr));
			GaLogger.sendException(
					"IOException happen while read date region of global lamrim schedule file.",
					e, true);
			return false;
		}

		Log.d(getClass().getName(), "GlobalLamrim range start: " + glRangeStart);
		Log.d(getClass().getName(), "GlobalLamrim range end: " + glRangeEnd);

		try {
			while (csvr.readRecord()) {
				int count = csvr.getColumnCount();
				GlRecord glr = new GlRecord();

				glr.dateStart = csvr.get(0);
				glr.dateEnd = csvr.get(1);
				glr.speechPositionStart = csvr.get(2);
				glr.speechPositionEnd = csvr.get(3);
				glr.totalTime = csvr.get(4);
				glr.theoryLineStart = csvr.get(5);
				glr.theoryLineEnd = csvr.get(6);
				glr.subtitleLineStart = csvr.get(7);
				glr.subtitleLineEnd = csvr.get(8);
				glr.desc = csvr.get(9);
				if (!addGlRecord(glr))
					return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView),
					getString(R.string.localGlobalLamrimScheduleFileDecodeErr));
			GaLogger.sendException("IOException happen while read data of global lamrim schedule file.",
					e, true);
			return false;
		}
		markScheduleDays();
		Log.d(getClass().getName(), "Total records: " + glSchedule.size());
		return true;
	}

	private boolean addGlRecord(GlRecord glr) {
		// DateFormat df = DateFormat.getDateInstance();
		Date startDate = null, endDate = null;
		String key = null;
		int length = 0;

		try {
			startDate = dateFormater.parse(glr.dateStart);
			endDate = dateFormater.parse(glr.dateEnd);
			length = (int) ((endDate.getTime() - startDate.getTime()) / 86400000) + 1;
		} catch (ParseException e) {
			e.printStackTrace();
			Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView),
					getString(R.string.localGlobalLamrimScheduleFileReadErr)+ "\"" + glr + "\"");
			GaLogger.sendException("Date format parse error: startDate="
					+ startDate + ", endDate=" + endDate, e, true);
			return false;
		}

		for (int i = 0; i < length; i++) {
			try{
				//startDate.setTime(startDate.getTime() + (i * 86400000));
				Calendar cal=Calendar.getInstance(Locale.getDefault());
				cal.setTimeInMillis(startDate.getTime() + (i * 86400000));
				startDate=cal.getTime();
				key = dateFormater.format(startDate);
				glSchedule.put(key, glr);
			}catch(ArrayIndexOutOfBoundsException aiobe){
				GaLogger.sendException("Locale="+Locale.getDefault().getDisplayCountry()+", Language="+Locale.getDefault().getDisplayLanguage()+", Date data=" + startDate.toString(), aiobe, true);
			}
			
		}
		Log.d(getClass().getName(), "Add record: key=" + key + ", data=" + glr);
		return true;
	}
	
	// If the dateFormater still error, then replace it by the function, check return null of the function!!
	private Date parseDate(String dateStr){
		String[] set=dateStr.split("/");
		if(set.length!=3){
			GaLogger.sendException("The format of date string error, it should be YYYY/MM/DD, string is ["+dateStr+"]", new Exception("Date Format Error"), true);
			return null;
		}
		
		int[] dateNum=new int[set.length];
		for(int i=0;i<set.length;i++){
			try{
				dateNum[i]=Integer.parseInt(set[i]);
			}catch(NumberFormatException nfe){
				GaLogger.sendException("The format of date string error, it should be number but error while parse integer, string is ["+dateStr+"]", nfe, true);
				return null;
			}
		}
		
		boolean isCorrect=true;
		isCorrect&=(dateNum[0]<2015);
		isCorrect&=(dateNum[1]<0) || (dateNum[1]>12);
		isCorrect&=(dateNum[2]<0) || (dateNum[2]>31);
		
		if(!isCorrect){
			GaLogger.sendException("The format of date string error, the range of number over it should be, string is ["+dateStr+"]", new Exception(), true);
			return null;
		}
		
		Calendar c=Calendar.getInstance(Locale.getDefault());
		c.set(dateNum[0], dateNum[1], dateNum[2]);
		return c.getTime();
	}

	private void showDownloadProgressDialog(){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						try {
							downloadPDialog.show();
							dialogShowing = true;
						} catch (Exception e) {
							GaLogger.sendException("Error happen while show download progress dialog.", e, false);
						}
					}
				}, 200);
			}
		});
	}

	private void dismissDownloadProgressDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					downloadPDialog.dismiss();
					dialogShowing = false;
				} catch (Exception e) {
					GaLogger.sendException("Error happen while dismiss download progress dialog.", e, false);
				}
			}
		});
	}

			private boolean downloadSchedule() {
				GoogleRemoteSource grs = new GoogleRemoteSource(getApplicationContext());
				String url = grs.getGlobalLamrimSchedule();
				String scheFileName = getString(R.string.globalLamrimScheduleFile);
				String tmpFileSub = getString(R.string.downloadTmpPostfix);
				String format = getString(R.string.globalLamrimScheduleFileFormat);
				File tmpFile = new File(getFilesDir() + File.separator + scheFileName
						+ tmpFileSub);
				File scheFile = new File(getFilesDir() + File.separator + scheFileName
						+ "." + format);

				showDownloadProgressDialog();

				Log.d(getClass().getName(), "Download " + url);
				HttpClient httpclient = getNewHttpClient();
				HttpGet httpget = new HttpGet(url);
				HttpResponse response = null;
				int respCode = -1;

				try {
					response = httpclient.execute(httpget);
					respCode = response.getStatusLine().getStatusCode();
					if (respCode != HttpStatus.SC_OK) {
						httpclient.getConnectionManager().shutdown();
						dismissDownloadProgressDialog();
						Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView), getString(R.string.dlgDescDownloadFail));
						return false;
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
					Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView),
							getString(R.string.dlgDescDownloadFail));
					if (downloadPDialog.isShowing()) {
						dismissDownloadProgressDialog();
					}
					return false;
				}

				Log.d(getClass().getName(), "Connect success, Downloading file.");
				HttpEntity httpEntity = response.getEntity();
				InputStream is = null;
				try {
					is = httpEntity.getContent();
				} catch (IllegalStateException e2) {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					httpclient.getConnectionManager().shutdown();
					e2.printStackTrace();
					Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView),
							getString(R.string.dlgDescDownloadFail));
					if (downloadPDialog.isShowing()) {
						dismissDownloadProgressDialog();
					}
					return false;
				} catch (IOException e2) {
					httpclient.getConnectionManager().shutdown();
					e2.printStackTrace();
					Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView),
							getString(R.string.dlgDescDownloadFail));
					if (downloadPDialog.isShowing()) {
						dismissDownloadProgressDialog();
					}
					return false;
				}

				final long contentLength = httpEntity.getContentLength();
				Log.d(getClass().getName(), "Content length: " + contentLength);
				FileOutputStream fos = null;
				int counter = 0;
				try {
					Log.d(getClass().getName(), "Create download temp file: " + tmpFile);
					fos = new FileOutputStream(tmpFile);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					GaLogger.sendException("Can't create temp file.", e, true);
					Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView), "無法建立暫存檔，請檢查磁碟空間是否足夠。");
					if (downloadPDialog.isShowing()) {
						dismissDownloadProgressDialog();
					}
					return false;
				}

				try {
					byte[] buf = new byte[getResources().getInteger(
							R.integer.downloadBufferSize)];
					int readLen = 0;
					Log.d(getClass().getName(), Thread.currentThread().getName()
							+ ": Start read stream from remote site, is="
							+ ((is == null) ? "NULL" : "exist") + ", buf="
							+ ((buf == null) ? "NULL" : "exist"));
					while ((readLen = is.read(buf)) != -1) {
						counter += readLen;
						fos.write(buf, 0, readLen);
					}
					is.close();
					fos.flush();
					fos.close();
				} catch (IOException e) {
					httpclient.getConnectionManager().shutdown();
					try {
						is.close();
					} catch (IOException e2) {
						e2.printStackTrace();
//				return false;
					}
					try {
						fos.close();
					} catch (IOException e2) {
						e2.printStackTrace();
//				return false;
					}
					tmpFile.delete();
					e.printStackTrace();
					Log.d(getClass().getName(), Thread.currentThread().getName()
							+ ": IOException happen while download media.");
					Util.showErrorPopupWindow(CalendarActivity.this, findViewById(R.id.rootView),
							getString(R.string.dlgDescDownloadFail));
					if (downloadPDialog.isShowing()) {
						dismissDownloadProgressDialog();
					}

					return false;
				}

		/*
		 * if(counter!=contentLength){
		 * httpclient.getConnectionManager().shutdown(); tmpFile.delete();
		 * showNarmalToastMsg(getString(R.string.dlgDescDownloadFail));
		 * downloadPDialog.dismiss(); return; }
		 */
				// rename the protected file name to correct file name
				if (scheFile.exists())
					scheFile.delete();
				tmpFile.renameTo(scheFile);
				httpclient.getConnectionManager().shutdown();
				Log.d(getClass().getName(), Thread.currentThread().getName()
						+ ": Download finish.");
				if (downloadPDialog.isShowing())
					dismissDownloadProgressDialog();
				return true;
			}

			/*
             * private void showDownloadProgDialog(){ runOnUiThread(new Runnable(){
             *
             * @Override public void run() { downloadPDialog =
             * ProgressDialog.show(CalendarActivity.this,
             * getString(R.string.dlgTitleDownloading),
             * String.format(getString(R.string.dlgDescDownloading),"",
             * getString(R.string.title_activity_calendar)), true); }}); }
             */
			private Intent getResultIntent(GlRecord glr) {
				Intent data = new Intent();
				data.putExtra("selectedDay", selectedDay);
				data.putExtra("dateStart", glr.dateStart);
				data.putExtra("dateEnd", glr.dateEnd);
				data.putExtra("speechPositionStart", glr.speechPositionStart);
				data.putExtra("speechPositionEnd", glr.speechPositionEnd);
				data.putExtra("totalTime", glr.totalTime);
				data.putExtra("theoryLineStart", glr.theoryLineStart);
				data.putExtra("theoryLineEnd", glr.theoryLineEnd);
				data.putExtra("subtitleLineStart", glr.subtitleLineStart);
				data.putExtra("subtitleLineEnd", glr.subtitleLineEnd);
				data.putExtra("desc", glr.desc);
				return data;
			}

			private HttpClient getNewHttpClient() {
				try {
					KeyStore trustStore = KeyStore.getInstance(KeyStore
							.getDefaultType());
					trustStore.load(null, null);

					SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
					sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

					HttpParams params = new BasicHttpParams();
					HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
					HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

					SchemeRegistry registry = new SchemeRegistry();
					registry.register(new Scheme("http", PlainSocketFactory
							.getSocketFactory(), 80));
					registry.register(new Scheme("https", sf, 443));

					ClientConnectionManager ccm = new ThreadSafeClientConnManager(
							params, registry);

					return new DefaultHttpClient(ccm, params);
				} catch (Exception e) {
					return new DefaultHttpClient();
				}
			}

			public class MySSLSocketFactory extends SSLSocketFactory {
				SSLContext sslContext = SSLContext.getInstance("TLS");

				public MySSLSocketFactory(KeyStore truststore)
						throws NoSuchAlgorithmException, KeyManagementException,
						KeyStoreException, UnrecoverableKeyException {
					super(truststore);

					TrustManager tm = new X509TrustManager() {
						public void checkClientTrusted(X509Certificate[] chain,
													   String authType) throws CertificateException {
						}

						public void checkServerTrusted(X509Certificate[] chain,
													   String authType) throws CertificateException {
						}

						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}
					};

					sslContext.init(null, new TrustManager[]{tm}, null);
				}

				@Override
				public Socket createSocket(Socket socket, String host, int port,
										   boolean autoClose) throws IOException, UnknownHostException {
					return sslContext.getSocketFactory().createSocket(socket, host,
							port, autoClose);
				}

				@Override
				public Socket createSocket() throws IOException {
					return sslContext.getSocketFactory().createSocket();
				}
			}
		}
