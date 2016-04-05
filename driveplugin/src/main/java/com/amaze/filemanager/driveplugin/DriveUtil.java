package com.amaze.filemanager.driveplugin;

/**
 * Created by Arpit on 05-11-2015.
 */

import android.os.AsyncTask;

import com.amaze.filemanager.ui.Layoutelements;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Arpit on 10/24/2015.
 */
public class DriveUtil {
    AsyncTask asyncTask = null;

    public DriveUtil() {
    }

    private static final String mFolderFields = "items(downloadUrl,fileSize,thumbnailLink,id,mimeType,modifiedDate,parents(id,isRoot),thumbnail/image,title),nextPageToken";

    public interface listReturn {
        void list(ArrayList<File> arrayList);

        void throwError(Exception e);
    }

    public void listFolder(final com.google.api.services.drive.Drive mService, final String mFolderId, final listReturn listReturn) {
     final ArrayList<com.google.api.services.drive.model.File> files = new ArrayList<>();
        if (asyncTask != null && asyncTask.getStatus() == AsyncTask.Status.RUNNING)
            asyncTask.cancel(true);
        asyncTask = new AsyncTask<Void, Integer, Void>() {
            Exception e=null;
            @Override
            public void onProgressUpdate(Integer... v){
                if(v!=null && v[0]==1 && e!=null)listReturn.throwError(e);
                else listReturn.list(files);
            }
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    String pg = "";
                    for (; ; ) {
                        Drive.Files.List lst = null;
                        lst = mService
                                .files()
                                .list()
                                .setQ("'" + mFolderId + "' in parents")
                                .setFields(mFolderFields)
                                .setMaxResults(100);

                        if (!pg.equals(""))
                            lst.setPageToken(pg);
                        final FileList fl = lst.execute();


                        if (fl.size() == 0)
                            break;
                        for (com.google.api.services.drive.model.File file : fl.getItems()) {
                            if (files.contains(file))
                                continue;
                            else {
                                files.add(file);
                            }
                        }
                        if (pg.equals(fl.getNextPageToken()))
                            break;
                        pg = fl.getNextPageToken();
                        if (pg == null || pg.equals(""))
                            break;
                    }
                    publishProgress(0);
                } catch (Exception e) {
                    this.e=e;
                    publishProgress(1);

                }
                return null;
            }
        }.execute();
    }
    public void listRoot(final com.google.api.services.drive.Drive mService,  final listReturn listReturn) {
        final ArrayList<com.google.api.services.drive.model.File> files = new ArrayList<>();
        if (asyncTask != null && asyncTask.getStatus() == AsyncTask.Status.RUNNING)
            asyncTask.cancel(true);
        asyncTask = new AsyncTask<Void, Integer, Void>() {
            Exception e=null;
            @Override
            public void onProgressUpdate(Integer... v){
                if(v!=null && v[0]==1 && e!=null)listReturn.throwError(e);
                else listReturn.list(files);
            }
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    String pg = "";
                    for (; ; ) {
                        Drive.Files.List lst = mService
                                .files()
                                .list()
                                .setFields(mFolderFields)
                                .setMaxResults(100);

                        if (!pg.equals(""))
                            lst.setPageToken(pg);
                         FileList fl;
                        try {
                            fl = lst.execute();
                        } catch (Exception e1) {
                            fl=null;
                            e1.printStackTrace();
                        }


                        if (fl.size() == 0)
                            break;
                        for (com.google.api.services.drive.model.File file : fl.getItems()) {
                            if (files.contains(file))
                                continue;
                            else if (isRoot(file)) {
                                files.add(file);
                            }
                        }
                        if (pg.equals(fl.getNextPageToken()))
                            break;
                        pg = fl.getNextPageToken();
                        if (pg == null || pg.equals(""))
                            break;
                    }
                    publishProgress(0);
                } catch (Exception e) {
                    this.e=e;
                    publishProgress(1);

                }
                return null;
            }
        }.execute();
    }

    public interface FileReturn {
        void getFile(File f);
    }

    ;

    public void getFile(final String id, final com.google.api.services.drive.Drive mService, final FileReturn fileReturn) throws UserRecoverableAuthIOException {
        new AsyncTask<Void, File, Void>() {
            @Override
            public void onProgressUpdate(File... f) {
                if (f != null)
                    fileReturn.getFile(f[0]);
            }


            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    publishProgress(mService.files().get(id).execute());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();


    }
    public File getParent(String folderid, Drive mSee) throws IOException, UserRecoverableAuthIOException {
        File f = mSee.files().get(folderid).execute();
        String id = null;
        for (ParentReference p : f.getParents()) {
            if (p.getId() != null) id = p.getId();
        }
        if (id == null) return null;
        return mSee.files().get(id).execute();
    }

    public List<Layoutelements> addToDrive(ArrayList<com.google.api.services.drive.model.File> mFile) {
        List<Layoutelements> a = new ArrayList<>();
        for (int i = 0; i < mFile.size(); i++) {

            com.google.api.services.drive.model.File f = mFile.get(i);
            String size = "";
            if (f.getMimeType().equals("application/vnd.google-apps.folder")) {
                a.add(new Layoutelements(null, f.getTitle(), f.getMimeType(), f.getId(), size,"", 0, false, "", true));
            } else {/*
                long longSize = 0;
                try {*//*
                    size = utils.readableFileSize((f.getFileSize()));
                    longSize = (f.getFileSize());
*//*
                } catch (NumberFormatException e) {
                    //e.printStackTrace();
                }*/
                try {

                    a.add(new Layoutelements(null, f.getTitle(),f.getMimeType(), f.getId(), size,"", 0, false, "", false));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return a;
    }
    public boolean isRoot(File folderId) {
        for (ParentReference p : folderId.getParents()) {
            if (p.getIsRoot())
                return true;
        }
        return false;
    }

    public interface GoBackCallback {
        void loadList(String id);
    }

    public void goBack(final String id, final GoBackCallback goBackCallback, final Drive mService) {
        new AsyncTask<Void, String, Void>() {
            @Override
            public void onProgressUpdate(String... f) {
                if (f == null) goBackCallback.loadList(null);
                else
                    goBackCallback.loadList(f[0]);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                if (mService == null || id == null) {
                    publishProgress(null);
                    return null;
                }
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches())
                    publishProgress(id);
                try {
                    File f = getParent(id, mService);
                    publishProgress(f != null ? f.getId() : null);


                } catch (Exception e) {
                    e.printStackTrace();
                    publishProgress(null);
                }
                return null;
            }
        }.execute();
    }


}

