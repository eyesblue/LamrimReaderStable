package eyes.blue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class FileSysManager {
//        public static String[] fileName=null;
//        public static int[] fileSize=null;
	static String logTag=null;
	static int NO_CACHE=0;
	public final static int INTERNAL=0;
	public final static int EXTERNAL=1;
	public final static String[] locateDesc={"internal","external"};
	final static int MEDIA_FILE=0;
	final static int SUBTITLE_FILE=1;
	final static int THEORY_FILE=2;
        
	static SharedPreferences runtime = null;
	static StatFs[] statFs=null;
	Context context=null;
//        static String[] remoteSite=null;
	static ArrayList<RemoteSource> remoteResources=new ArrayList<RemoteSource>();
	static DiskSpaceFullListener diskFullListener=null;

	DownloadListener downloadListener=null;
	static GoogleRemoteSource grs=null;
	static String srcRoot[] = new String[2];

//        static int bufLen=16384;
	
	public FileSysManager(Context context){
		runtime = context.getSharedPreferences(context.getString(R.string.runtimeStateFile), 0);
		
		statFs=new StatFs[2];
		statFs[INTERNAL]=new StatFs(Environment.getRootDirectory().getAbsolutePath());
		statFs[EXTERNAL]=new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
		srcRoot[INTERNAL]=context.getFileStreamPath(context.getString(R.string.filePathRoot)).getAbsolutePath();
		boolean extWritable=(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
		if(extWritable && context.getExternalFilesDir(null)!=null)
			srcRoot[EXTERNAL]=context.getExternalFilesDir(context.getString(R.string.filePathRoot)).getAbsolutePath();

		FileSysManager.logTag=getClass().getName();
		this.context=context;
		grs=new GoogleRemoteSource(context);
	}
	
	public FileSysManager(Service downloadAllService) {
		// TODO Auto-generated constructor stub
	}

	public String getSysDefMediaDir(){
        boolean extWritable=(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
        if(extWritable && context.getExternalFilesDir(null)!=null)
        	return srcRoot[EXTERNAL]+File.separator+context.getString(R.string.audioDirName);
        return srcRoot[INTERNAL]+File.separator+context.getString(R.string.audioDirName);
}

	
	public String getLocateDir(int locate, int type){
		int mediaType=context.getResources().getInteger(R.integer.MEDIA_TYPE);
		int subtitleType=context.getResources().getInteger(R.integer.SUBTITLE_TYPE);
		String audioDirName=context.getString(R.string.audioDirName);
		String subtitleDirName=context.getString(R.string.subtitleDirName);
		
		if(locate == EXTERNAL){
			boolean extWritable=(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
			if(!extWritable || context.getExternalFilesDir(null) == null)
				return null;
		}

		if(type == mediaType)
			return srcRoot[locate]+File.separator+audioDirName;
		else if(type == subtitleType)
			return srcRoot[locate]+File.separator+subtitleDirName;
		
		// shouldn't into bellow line.
		return null;
	}
	
	public void setDownloadListener(DownloadListener listener){
		this.downloadListener=listener;
	}
        
        /*
         * the file structure is follow
         * [PackageDir]\[AppName](LamrimReader)\{Audio,Book,Subtitle}
         * */
        public void checkFileStructure(){
        	boolean extWritable=(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
        	File appRoot=null;
        	
        	// Make file structure of external storage.
        	if(extWritable && srcRoot[EXTERNAL] != null){
        		appRoot=new File(srcRoot[EXTERNAL]);
        		if(appRoot.isFile()){
            		appRoot.delete();
            		appRoot.mkdirs();
            	}
        		
        		String[] dirs={context.getString(R.string.audioDirName),context.getString(R.string.subtitleDirName),context.getString(R.string.theoryDirName)};
            	for(String s:dirs){
            		File subDir=new File(srcRoot[EXTERNAL]+File.separator+s);
            		if(subDir.isFile())subDir.delete();
            		if(!subDir.exists())subDir.mkdirs();
            	}
        	}
        	
        	// Make file structure of internal storage
        	appRoot=new File(srcRoot[INTERNAL]);
        	
        	// The app root should not be a file, that will be cause app error
        	if(appRoot.isFile()){
        		appRoot.delete();
        		appRoot.mkdirs();
        	}
        	
        	String[] dirs={context.getString(R.string.audioDirName),context.getString(R.string.subtitleDirName),context.getString(R.string.theoryDirName)};
        	for(String s:dirs){
        		File subDir=new File(srcRoot[INTERNAL]+File.separator+s);
        		if(subDir.isFile())subDir.delete();
        		if(!subDir.exists())subDir.mkdirs();
        	}
        	
        	boolean hasCheckedOlderSubtitle=runtime.getBoolean(context.getString(R.string.checkSubtitleV20141127), false);
        	if(!hasCheckedOlderSubtitle)
        		deleteOlderSubtitle();
        	
/*        	// Move old files(LamrimReader/{audio,subtitle,theory} to new direct(廣論App/{audio,subtitle,theory}).
        	File oldDirRoot=new File(srcRoot[INTERNAL]+File.separator+dirs[0]).getParentFile().getParentFile();
        	Log.d("FileSysManager","Pkg dir: "+oldDirRoot.getAbsolutePath());
        	oldDirRoot=new File(oldDirRoot.getAbsolutePath()+File.separator+"LamrimReader");
        	for(String s:dirs){
        		File oldSubDir=new File(oldDirRoot+File.separator+s);
        		File subDir=new File(srcRoot[INTERNAL]+File.separator+s);
        		subDir.delete();
        		oldSubDir.renameTo(subDir);
        	}
*/		}
        
        private void deleteOlderSubtitle(){
        	new Thread(new Runnable(){
				@Override
				public void run() {
					long date=1417216380000L;// 2014/11/28 23:13
		        	for(int i=0;i<SpeechData.name.length;i++){
		        		File f=getLocalSubtitleFile(i);
		        		if(f==null || !f.exists())continue;
		        		if(f.lastModified()<date){
		        			Log.d("FileSysManager","Delete Older subtitle "+SpeechData.getSubtitleName(i));
		        			f.delete();
		        		}
		        	}
		        	
		        	SharedPreferences.Editor editor=runtime.edit();
		        	editor.putBoolean(context.getString(R.string.checkSubtitleV20141127), true);
		        	editor.commit();
		        	
				}}).start();
        }
        /*
         * The location of media file is [PackageDir]\[AppName](LamrimReader)\Audio
         * If the file exist, return the exist file no matter internal or external,
         * if file not exist, return the allocate place of the file, allocate in external first, if not, return internal file.
         * */
        public File getLocalMediaFile(int i){
        	// Check the directory by user specify.
        	boolean useThirdDir=runtime.getBoolean(context.getString(R.string.isUseThirdDir), false);
        	String userSpecDir=runtime.getString(context.getString(R.string.userSpecifySpeechDir),null);
  
        	File specFile=null, extF=null, intF=null;
        	if(useThirdDir && userSpecDir!=null){
        		specFile=new File(userSpecDir+File.separator+SpeechData.name[i]);
        		// Test is exist and readable.
        		if(specFile.exists() && specFile.canRead()){
        			Log.d(logTag,Thread.currentThread().getName()+": the media file exist in user specification location: "+specFile.getAbsolutePath());
        			return specFile;
        		}
        	}
        	
        	if(isExtMemWritable() && srcRoot[EXTERNAL] != null){
        		extF=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
        		//extF=context.getExternalFilesDir(context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
//        		Log.d(logTag,"Check exist: "+extF.getAbsolutePath());
        		if(extF.exists())return extF;
        	}
        	intF=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
        	//intF=new File(context.getFilesDir()+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
//    		Log.d(logTag,"Check exist: "+intF.getAbsolutePath());
    		if(intF.exists())return intF;

    		
    		// Test is user specify locate writable.
    		if(useThirdDir){
    			File dir=new File(userSpecDir);
    			if(dir.exists() && dir.canWrite())
    				return specFile;
    		}
    		// Check is there enough space for save the file
    		int reserv=context.getResources().getIntArray(R.array.mediaFileSize)[i];
			reserv+=reserv*context.getResources().getInteger(R.integer.reservSpacePercent);
    		if(isExtMemWritable()){
//    			Log.d(logTag,"File not exist return user external place");
    			if(getFreeMemory(EXTERNAL)>reserv)
    				return extF;
    		}
//    		Log.d(logTag,"File not exist return user internal place");
    		if(getFreeMemory(INTERNAL)>reserv)
    			return intF;
    		
    		return null;
        }
      
        public File getLocalMediaFileSavePath(int i){
        	boolean useThirdDir=runtime.getBoolean(context.getString(R.string.isUseThirdDir), false);
        	String userSpecDir=runtime.getString(context.getString(R.string.userSpecifySpeechDir),null);
        	
        	if(useThirdDir && userSpecDir!=null)
        		return new File(userSpecDir+File.separator+SpeechData.name[i]);
        	
        	if(isExtMemWritable() && srcRoot[EXTERNAL] != null)
        		return new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
        	
        	return new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
        }
        
        /*
         * The location of media file is [PackageDir]\[AppName](LamrimReader)\Audio
         * If the file exist, return the exist file no matter internal or external,
         * if file not exist, return the allocate place of the file, allocate in external first, if not, return internal file.
         * */
        public File getLocalSubtitleFile(int i){
        	File extF=null, intF=null;
        	if(isExtMemWritable() && srcRoot[EXTERNAL] != null){
/*        		Log.d(getClass().getName(),"srcRoot[external]="+srcRoot[EXTERNAL]);
        		Log.d(getClass().getName(),"context="+context);
        		Log.d(getClass().getName(),"subtitleDirName="+context.getString(R.string.subtitleDirName));
        		Log.d(getClass().getName(),"SpeechData.getSubtitleName(i)="+SpeechData.getSubtitleName(i));
        		Log.d(getClass().getName(),"context.getString(R.string.defSubtitleType)="+context.getString(R.string.defSubtitleType));
*/        		
        		extF= new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        		if(extF.exists())return extF;
        	}
        	intF=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        	if(intF.exists())return intF;
        	
        	
        	int reserv=context.getResources().getInteger(R.integer.subtitleReservSizeK);
        	if(srcRoot[EXTERNAL] != null && getFreeMemory(EXTERNAL)>reserv)
        		return extF;
        	if(getFreeMemory(INTERNAL)>reserv)
        		return intF;
        	
        	return null;
        }
        
        public File getLocalSubtitleFileSavePath(int i){
        	if(isExtMemWritable() && srcRoot[EXTERNAL] != null)
        		return new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        	
        	return new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        }
        
        public File[] getMediaFileList(int locate){
        	String dir=context.getString(R.string.audioDirName);

        	File srcDir=new File(srcRoot[locate]+File.separator+dir);
        	return srcDir.listFiles();
        }

	public File getSubtitleSearchCacheFile(){
		String name=context.getString(R.string.subtitleSearchCache);
		File extF=null, intF=null;
		if(isExtMemWritable() && srcRoot[EXTERNAL] != null){
			extF= new File(srcRoot[EXTERNAL]+File.separator+name);
			if(extF.exists())return extF;
		}
		intF=new File(srcRoot[INTERNAL]+File.separator+name);
		if(intF.exists())return intF;

		if(extF!=null)return extF;
		return intF;
	}
        
        public void deleteAllSpeechFiles(int locate){
        	Log.d("FileSysManager","Delete all speech file in "+locateDesc[locate]);
        	String dir=context.getString(R.string.audioDirName);

        	File srcDir=new File(srcRoot[locate]+File.separator+dir);
        	for(File f:srcDir.listFiles())
        		f.delete();

        }
        
        public void deleteAllSubtitleFiles(int locate){
        	Log.d("FileSysManager","Delete all subtitle file in "+locateDesc[locate]);
        	String dir=context.getString(R.string.subtitleDirName);
       		File srcDir=new File(srcRoot[locate]+File.separator+dir);
       		for(File f:srcDir.listFiles())
       			f.delete();
        }
        
        /*
         * Not test yet.
         * Move all files of INTERNAL or EXTERNAL to EXTERNAL or INTERNAL
         * */
        public void moveAllFilesTo(int from,int to,final CopyListener listener){
        	final ProgressDialog pd= new ProgressDialog(context);
        	pd.setCancelable(false);
        	pd.setTitle("檔案搬移");
        	
        	final AsyncTask<Integer, Void, Void> executer=new  AsyncTask<Integer, Void, Void>(){
				@Override
				protected Void doInBackground(Integer... params) {
					int from=params[0];
					int to=params[1];
					final String[] dirs={context.getString(R.string.audioDirName),context.getString(R.string.subtitleDirName)};
					
                	for(int j=0;j<dirs.length;j++){
                		String s=dirs[j];
                		File srcDir=new File(srcRoot[from]+File.separator+s);
                		String destDirStr=srcRoot[to]+File.separator+s;
                		
                		File destDir = new File(destDirStr);
                		if(!moveContentsOfDir(srcDir, destDir, pd))
                			if(listener!=null)listener.copyFail(srcDir,destDir);
                	}
                	if(listener!=null)listener.copyFinish();
                	pd.dismiss();
					return null;
				}
				
				@Override
        		protected void onCancelled(){
					if(listener!=null)listener.userCancel();
        		}
			};
        	
			executer.execute(from,to);

    		pd.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
    		    @Override
    		    public void onClick(DialogInterface dialog, int which) {
    		    	executer.cancel(false);
    		    	dialog.dismiss();
    		    }
    		});
    		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    		pd.show();
        }
        
        public boolean moveAllMediaFileToUserSpecifyDir(File destDir, ProgressDialog pd){
        	File intDir=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.audioDirName));
        	if(!moveContentsOfDir(intDir,destDir,pd))return false;
        	if(srcRoot[EXTERNAL] != null){
        		File extDir=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.audioDirName));
        		if(!moveContentsOfDir(extDir,destDir,pd))return false;
        	}
        	
        	return true;
        }
        
        private boolean moveContentsOfDir(File srcDir, File destDir, final ProgressDialog pd){
        	final File[] files=srcDir.listFiles();
        	Log.d(logTag,"There are "+files.length+" files wait for move.");
    		((Activity)context).runOnUiThread(new Runnable(){
    			@Override
    			public void run(){
    				if(pd!=null)pd.setMax(files.length);
//            		pd.setSecondaryProgress((int) (((double)progress/dirs.length)*files.length));
    			}
    		});
        	// Check is the destination has the same file, delete source one.
    		for(final File src: files){
    			final File dist=new File(destDir.getAbsolutePath()+File.separator+src.getName());
    			if(dist.exists()){
    				if(src.length()==dist.length()){
    					src.delete();
    					if(pd!=null)pd.setProgress(pd.getProgress()+1);
    					continue;
    				}
    			}

    			/* Copy To */
    			((Activity)context).runOnUiThread(new Runnable(){
					@Override
					public void run() {
						pd.setMessage("移動"+src.getAbsolutePath()+" 到 "+dist.getAbsolutePath());
					}});
    			if(!src.renameTo(dist))
    				if(!moveFile(src, dist))
    					return false;
 
    			((Activity)context).runOnUiThread(new Runnable(){
					@Override
					public void run() {
						pd.setProgress(pd.getProgress()+1);
					}});
    		}
    		return true;
        }
        
        private boolean moveFile(File from, File to){
        	File distTemp=new File(to.getAbsolutePath()+context.getString(R.string.downloadTmpPostfix));
			FileInputStream fis = null;
			FileOutputStream fos = null;
			Log.d(logTag,"Copy "+from.getAbsolutePath()+" to "+to.getAbsolutePath());
			try {
				fis = new FileInputStream(from);
				fos =new FileOutputStream(distTemp);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			}
			
			byte[] buf=new byte[context.getResources().getInteger(R.integer.downloadBufferSize)];
			int readLen=0;
			
			try {
				while((readLen=fis.read(buf))!=-1)
					fos.write(buf, 0, readLen);
				
				fis.close();
    			fos.flush();
    			fos.close();
    			
				to.delete();
    			distTemp.renameTo(to);
    			from.delete();
			} catch (IOException e) {
				distTemp.delete();
				e.printStackTrace();
				return false;
			}
			return true;
        }
        
        public void maintainStorages(){
        	File userSpecDir=null;
        	boolean isUseThirdDir=runtime.getBoolean(context.getString(R.string.isUseThirdDir), false);
        	if(isUseThirdDir){
        		userSpecDir=new File(runtime.getString(context.getString(R.string.userSpecifySpeechDir), null));
        		if(!userSpecDir.exists())userSpecDir=null;
        	}
        	// we must make sure the userSpecDir exist, because of remove SD card, the dir will not exist.

        	
        	// Check duplication files.
        	for(int i=0;i<SpeechData.name.length;i++){
        		File meu=null;
        		
        		if(userSpecDir!=null)meu=new File(userSpecDir.getAbsolutePath()+File.separator+SpeechData.name[i]);
        		
        		File mei=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
        		File sbi=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        		File mex=null, sbx=null;
        		if(srcRoot[EXTERNAL] != null){
        			mex=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
        			sbx=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        		}
        		if(meu!=null && meu.exists()){
        			Log.d(logTag,SpeechData.getNameId(i)+" Media file exist in USER SPECIFY DIR, delete external and internal.");
        			if(srcRoot[EXTERNAL] != null)mex.delete();
        			mei.delete();
        		}
        		else if(srcRoot[EXTERNAL] != null && mex.exists()){
        			Log.d(logTag,SpeechData.getNameId(i)+" Media file exist in EXTERNAL DIR, delete internal.");
        			mei.delete();
        		}
        		
        		if(srcRoot[EXTERNAL] != null && sbx.exists()){
        			Log.d(logTag,SpeechData.getNameId(i)+" Subtitle file exist in EXTERNAL DIR, delete internal.");
        			sbi.delete();
        		}
        	}
        	
        	File miDir=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.audioDirName));
        	File siDir=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.subtitleDirName));
        	File meDir=null, seDir=null;
        	if(srcRoot[EXTERNAL] != null){
        		meDir=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.audioDirName));
        		seDir=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.subtitleDirName));
        	}
        	FilenameFilter filter= new FilenameFilter (){
				@Override
				public boolean accept(File dir, String filename) {
					if(filename.endsWith(context.getResources().getString(R.string.downloadTmpPostfix)))
						return true;
					return false;
				}};

			// Delete subtitle search cache file, execute 2 times, if both external and internal storage has one.
			File cacheFile=getSubtitleSearchCacheFile();
			if(cacheFile.exists())cacheFile.delete();
			cacheFile=getSubtitleSearchCacheFile();
			if(cacheFile.exists())cacheFile.delete();
			
			// Delete temp files.
			File[] files=null;
			if((files=miDir.listFiles(filter))!=null)for(File f:files)f.delete();
			if(srcRoot[EXTERNAL] != null && (files=meDir.listFiles(filter))!=null)for(File f:files)f.delete();
			if((files=siDir.listFiles(filter))!=null)for(File f:files)f.delete();
			if(srcRoot[EXTERNAL] != null && (files=seDir.listFiles(filter))!=null)for(File f:files)f.delete();
			if(userSpecDir!=null)if((files=userSpecDir.listFiles(filter))!=null)for(File f:files)f.delete();
        }
        
        // NOT test yet
        public File getLocalTheoryFile(int i){
        	if(srcRoot[EXTERNAL] != null && isExtMemWritable())return new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.theoryDirName)+File.separator+SpeechData.getTheoryName(i)+"."+context.getString(R.string.defTheoryType));
        	else return new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.theoryDirName)+File.separator+SpeechData.getTheoryName(i)+"."+context.getString(R.string.defTheoryType));
        }

/*        public boolean isFileValid(int i,int resType){
//        	Log.d(logTag,Thread.currentThread().getName()+":Check the existed file");
        	File file=null;
        	

        	if(resType==context.getResources().getInteger(R.integer.MEDIA_TYPE)){
        		file=getLocalMediaFile(i);
        		if(file==null||!file.exists())return false;
        		
        		// First check is the file from user specified dir, then no size or crc check,
        		// return true if file exist.
        		//boolean isUseThirdDir=runtime.getBoolean(context.getString(R.string.isUseThirdDir), false);
        		String userSpecDir=runtime.getString(context.getString(R.string.userSpecifySpeechDir), null);
        		if(userSpecDir != null)
        		if(file.getAbsolutePath().startsWith(userSpecDir)){
        			Log.d(logTag,"The file is from user specified folder, no size or crc check.");
        			if(userSpecDir!=null && file.exists()){
        				return true;
        			}
        			else {
//        				Log.d(logTag,"The user specified file is not exist.");
        				return false;
        			}
        		}
        		
        		
        		
        		int size=SpeechData.length[i];
        		if(file.length()!=size){
            		Log.d(logTag,"The size of file is not correct, should be "+size+", but "+file.length());
            		return false;
            	}
            	try {
            		
    				return Util.isFileCorrect(file, SpeechData.crc[i]);
    			} catch (Exception e) {
    				e.printStackTrace();
    				return false;
    			}
        	}
        	else if(resType==context.getResources().getInteger(R.integer.SUBTITLE_TYPE)){
        		file=getLocalSubtitleFile(i);
        		if(file==null||file.exists())return true;
//            	Log.d(logTag,"The subtitle file "+file.getAbsolutePath()+" is not exist");
            	return false;
        	}
        	else if(resType==context.getResources().getInteger(R.integer.THEORY_TYPE)){
        		file=getLocalTheoryFile(i);
        		if(file.exists())return true;
            	Log.d(logTag,"The theory file "+file.getAbsolutePath()+" is not exist");
            	return false;
        	}
        	
        	Log.e(logTag,"FileSysManager.isFileValid(): Logical error: the resource type shouldn't "+resType);
        	return false;
        }
        */
        public boolean isFromUserSpecifyDir(File speechFile){
        	boolean extWritable=(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
        	if(speechFile.getAbsolutePath().startsWith(srcRoot[INTERNAL]))return false;
        	if(srcRoot[EXTERNAL] != null && extWritable && speechFile.getAbsolutePath().startsWith(srcRoot[EXTERNAL]))return false;
        	return true;
        }
        
        public boolean isExtMemWritable(){
        	return (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
        }
        
        public long getTotalMemory(int locate)
        {
        	if(statFs[locate]==null)return 0;
        	return ((long)statFs[locate].getBlockCount() * (long)statFs[locate].getBlockSize());
        }

        public long getFreeMemory(int locate)
        {
        	if(statFs[locate]==null)return 0;
        	return ((long)statFs[locate].getAvailableBlocks() * (long)statFs[locate].getBlockSize());
        }
        
        public int getGlobalUsage(int locate){
        	if(statFs[locate]==null)return 0;
        	double result=statFs[locate].getAvailableBlocks();
        	result/=statFs[locate].getBlockCount();
        	return (int) (result*100);
        }
        
        public long getAppUsed(int locate){
        	if(srcRoot[locate]==null)return 0;
        	long total=0;
        	String[] dirs={context.getString(R.string.audioDirName),context.getString(R.string.subtitleDirName)};
        	
        	for(String s: dirs){
        		File dir=new File(srcRoot[locate]+File.separator+s);
        		if(!dir.exists() || !dir.isDirectory())continue;
        		File[] fs=dir.listFiles();
        		for(File f:fs)
        			total+=f.length();
        	}
        	return total;
        }
        
        public String getSrcRootPath(int dir){
        	return srcRoot[dir];
        }
        
        public void setDiskSpaceFullListener(DiskSpaceFullListener dsfl){
            this.diskFullListener=dsfl;
        }
    
        public boolean isFilesReady(int id){
        	File media = getLocalMediaFile(id);
    		File subtitle = getLocalSubtitleFile(id);
    		if(media == null || subtitle == null || !media.exists() || !subtitle.exists())
    			return false;
    		return true;
        }
        /*
         * Given the media ids you wish to check that is those ready, return the unready ids. 
         * */
        public int[] getUnreadyList(int ...ids){
        	ArrayList<Integer> list=new ArrayList<Integer>();
        	
        	for(int i:ids){
        		File media = getLocalMediaFile(i);
        		File subtitle = getLocalSubtitleFile(i);
        		if(media == null || subtitle == null || !media.exists() || !subtitle.exists())
        			list.add(i);
        	}
        	if(list.size()==0)return null;
        	
        	int[] ia=new int[list.size()];
        	for(int i=0;i<list.size();i++)
        		ia[i]=list.get(i);
        	return ia;
    	}

        
    class DiskSpaceFullListener{
        public void diskSpaceFull(){}
    }
}
