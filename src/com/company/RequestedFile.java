package com.company;

import java.io.File;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by der-schuessel on 11.10.16.
 */
public class RequestedFile {
    private String fileName;
    private String path;
    private long lastModified;

    public RequestedFile(File file) {
        lastModified = file.lastModified();
        fileName = file.getName();
        path = file.getAbsolutePath();
    }

    public String getETag() {
        return String.valueOf(hashCode());
    }

    public String getLastModifiedTag() {
        return DateTimeFormatter.RFC_1123_DATE_TIME
                .withZone(ZoneId.of("GMT"))
                .format(FileTime.from(lastModified, TimeUnit.MILLISECONDS).toInstant());
    }

    public boolean isFileNewerThanTag(String lastModifiedTag) {
        LocalDateTime date = LocalDateTime.parse(lastModifiedTag, DateTimeFormatter.RFC_1123_DATE_TIME);
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.of("GMT"));
        return lastModified > zonedDateTime.toInstant().toEpochMilli();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestedFile that = (RequestedFile) o;

        if (lastModified != that.lastModified) return false;
        if (!fileName.equals(that.fileName)) return false;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = fileName.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + (int) (lastModified ^ (lastModified >>> 32));
        return result;
    }

}
