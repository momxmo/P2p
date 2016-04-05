package com.amaze.filemanager.ui;

/**
 * Created by Arpit on 04-11-2015.
 */
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;


public class Layoutelements implements Parcelable {
    public Layoutelements(Parcel im) {
        try {
            Bitmap bitmap = (Bitmap) im.readParcelable(getClass().getClassLoader());
            // Convert Bitmap to Drawable:
            imageId = new BitmapDrawable(bitmap);


            title = im.readString();
            desc = im.readString();
            permissions = im.readString();
            symlink = im.readString();
            int j = im.readInt();
            date = im.readLong();
            int i = im.readInt();
            if (i == 0) {
                header = false;
            } else {
                header = true;
            }
            if (j == 0) {
                isDirectory = false;
            } else {
                isDirectory = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        date1 = im.readString();
        longSize = im.readLong();
    }


    public int describeContents() {
        // TODO: Implement this method
        return 0;
    }


    public void writeToParcel(Parcel p1, int p2) {
        p1.writeString(title);
        p1.writeString(desc);
        p1.writeString(permissions);
        p1.writeString(symlink);
        p1.writeInt(isDirectory ? 1 : 0);
        p1.writeLong(date);
        p1.writeInt(header ? 1 : 0);
        p1.writeParcelable(((BitmapDrawable) imageId).getBitmap(), p2);
        p1.writeString(date1);
        p1.writeLong(longSize);
        // TODO: Implement this method
    }


    private Drawable imageId;
    private String title;
    private String desc;
    private String permissions;
    private String symlink;
    private String size;
    private boolean isDirectory;
    private long date = 0, longSize = 0;
    private String date1 = "";
    private boolean header;


    public Layoutelements(Drawable imageId, String title, String desc, String permissions, String symlink, String size, long longSize, boolean header, String date, boolean isDirectory) {
        this.imageId = imageId;
        this.title = title;
        this.desc = desc;
        this.permissions = permissions.trim();
        this.symlink = symlink.trim();
        this.size = size;
        this.header = header;
        this.longSize = longSize;
        this.isDirectory = isDirectory;
        if (!date.trim().equals("")) {
            this.date = Long.parseLong(date);
            this.date1 = getdate(this.date, "MMM dd, yyyy", "15");
        }
    }
    public String getdate(long f,String form,String year) {

        SimpleDateFormat sdf = new SimpleDateFormat(form);
        String date=(sdf.format(f)).toString();
        if(date.substring(date.length()-2,date.length()).equals(year))
            date=date.substring(0,date.length()-6);
        return date;
    }



    public static final Creator<Layoutelements> CREATOR =
            new Creator<Layoutelements>() {
                public Layoutelements createFromParcel(Parcel in) {
                    return new Layoutelements(in);
                }


                public Layoutelements[] newArray(int size) {
                    return new Layoutelements[size];
                }
            };


    public Drawable getImageId() {
        return imageId;
    }


    public void setImageId(Drawable imageId) {
        this.imageId = imageId;
    }

    public String getDesc() {
        return desc.toString();
    }


    public String getTitle() {
        return title.toString();
    }


    public boolean isDirectory() {
        return isDirectory;
    }


    public String getSize() {
        return size;
    }

    public long getlongSize() {
        return longSize;
    }


    public String getDate() {
        return date1;
    }


    public long getDate1() {
        return date;
    }


    public String getPermissions() {
        return permissions;
    }

}