/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package com.gallatinsystems.survey.device.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.domain.FileTransmission;
import com.gallatinsystems.survey.device.domain.QuestionResponse;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.domain.SurveyInstance;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.util.Base32;
import com.gallatinsystems.survey.device.util.ConstantUtil;

/**
 * Database class for the survey db. It can create/upgrade the database as well
 * as select/insert/update survey responses. TODO: break this up into separate
 * DAOs
 * 
 * @author Christopher Fagiani
 */
public class SurveyDbAdapter {
    public static final String QUESTION_FK_COL = "question_id";
    public static final String ANSWER_COL = "answer_value";
    public static final String ANSWER_TYPE_COL = "answer_type";
    public static final String SURVEY_RESPONDENT_ID_COL = "survey_respondent_id";
    public static final String RESP_ID_COL = "survey_response_id";
    public static final String SURVEY_FK_COL = "survey_id";
    public static final String PK_ID_COL = "_id";
    public static final String USER_FK_COL = "user_id";
    public static final String DISP_NAME_COL = "display_name";
    public static final String EMAIL_COL = "email";
    public static final String SUBMITTED_FLAG_COL = "submitted_flag";
    public static final String SUBMITTED_DATE_COL = "submitted_date";
    public static final String SURVEY_START_COL = "survey_start";
    public static final String DELIVERED_DATE_COL = "delivered_date";
    public static final String CREATED_DATE_COL = "created_date";
    public static final String UPDATED_DATE_COL = "updated_date";
    public static final String PLOT_FK_COL = "plot_id";
    public static final String LAT_COL = "lat";
    public static final String LON_COL = "lon";
    public static final String ELEVATION_COL = "elevation";
    public static final String DESC_COL = "description";
    public static final String STATUS_COL = "status";
    public static final String VERSION_COL = "version";
    public static final String TYPE_COL = "type";
    public static final String LOCATION_COL = "location";
    public static final String FILENAME_COL = "filename";
    public static final String KEY_COL = "key";
    public static final String VALUE_COL = "value";
    public static final String DELETED_COL = "deleted_flag";
    public static final String MEDIA_SENT_COL = "media_sent_flag";
    public static final String HELP_DOWNLOADED_COL = "help_downloaded_flag";
    public static final String LANGUAGE_COL = "language";
    public static final String SURVEY_GROUP_ID_COL = "survey_group_id";
    public static final String SURVEYED_LOCALE_ID_COL = "surveyed_locale_id";
    public static final String SAVED_DATE_COL = "saved_date";
    public static final String COUNTRY_COL = "country";
    public static final String PROP_NAME_COL = "property_names";
    public static final String PROP_VAL_COL = "property_values";
    public static final String INCLUDE_FLAG_COL = "include_flag";
    public static final String SCORED_VAL_COL = "scored_val";
    public static final String STRENGTH_COL = "strength";
    public static final String TRANS_START_COL = "trans_start_date";
    public static final String EXPORTED_FLAG_COL = "exported_flag";
    public static final String UUID_COL = "uuid";
    
    public interface Tables {
        String SURVEY = "survey";
        String RESPONDENT = "survey_respondent";
        String RESPONSE = "survey_response";
        String USER = "user";
        String PLOT = "plot";
        String PLOT_POINT = "plot_point";
        String PREFERENCES = "preferences";
        String POINT_OF_INTEREST = "point_of_interest";
        String TRANSMISSION_HISTORY = "transmission_history";
        String SURVEY_GROUP = "survey_group";// Introduced in Point Updates
        String SURVEYED_LOCALE = "surveyed_locale";// Introduced in Point Updates
        String SYNC_TIME = "sync_time";// Introduced in Point Updates
    }
    
    public interface SurveyGroupAttrs {
        String ID                 = "_id";
        String NAME               = "name";
        String REGISTER_SURVEY_ID = "register_survey_id";
        String MONITORED          = "monitored";
    }
    
    public interface SurveyedLocaleAttrs {
        String ID                 = "_id";
        String SURVEYED_LOCALE_ID = "surveyed_locale_id";
        String SURVEY_GROUP_ID    = "survey_group_id";
        String NAME               = "name";
        String LATITUDE           = "latitude";
        String LONGITUDE          = "longitude";
    }
    
    public interface SyncTimeAttrs {
        String ID                 = "_id";
        String SURVEY_GROUP_ID    = "survey_group_id";
        String TIME               = "time";
    }

    private static final String TAG = "SurveyDbAdapter";
    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    /**
     * Database creation sql statement
     */
    private static final String SURVEY_TABLE_CREATE = "create table survey (_id integer primary key, display_name text not null, "
            + "version real, type text, location text, filename text, language, help_downloaded_flag text, deleted_flag text, survey_group_id integer);";

    private static final String SURVEY_RESPONDENT_CREATE = "create table survey_respondent (_id integer primary key autoincrement, "
            + "survey_id integer not null, submitted_flag text, submitted_date text, delivered_date text, user_id integer, media_sent_flag text, "
            + "status text, saved_date long, exported_flag text, uuid text, survey_start integer, surveyed_locale_id text);";

    private static final String SURVEY_RESPONSE_CREATE = "create table survey_response (survey_response_id integer primary key autoincrement, "
            + " survey_respondent_id integer not null, question_id text not null, answer_value text not null, answer_type text not null, include_flag text not null, scored_val text, strength text);";

    private static final String USER_TABLE_CREATE = "create table user (_id integer primary key autoincrement, display_name text not null, email text not null, deleted_flag text);";

    private static final String PLOT_TABLE_CREATE = "create table plot (_id integer primary key autoincrement, display_name text, description text, created_date text, user_id integer, status text);";

    private static final String PLOT_POINT_TABLE_CREATE = "create table plot_point (_id integer primary key autoincrement, plot_id integer not null, lat text, lon text, elevation text, created_date text);";

    private static final String PREFERENCES_TABLE_CREATE = "create table preferences (key text primary key, value text);";

    private static final String POINT_OF_INTEREST_TABLE_CREATE = "create table point_of_interest (_id integer primary key, country text, display_name text, lat real, lon real, property_names text, property_values text, type text, updated_date integer);";

    private static final String TRANSMISSION_HISTORY_TABLE_CREATE = "create table transmission_history (_id integer primary key, survey_respondent_id integer not null, status text, filename text, trans_start_date long, delivered_date long);";
    
    private static final String SURVEY_GROUP_TABLE_CREATE = "create table survey_group (_id integer primary key on conflict replace, name text, register_survey_id text, monitored integer);";
    
    private static final String SURVEYED_LOCALE_TABLE_CREATE = "create table surveyed_locale (_id integer primary key autoincrement, surveyed_locale_id text, survey_group_id integer, name text, latitude real, longitude real, "
    		+ " UNIQUE(surveyed_locale_id) ON CONFLICT REPLACE);";
    
    private static final String SYNC_TIME_TABLE_CREATE = "CREATE TABLE sync_time (_id INTEGER PRIMARY KEY AUTOINCREMENT, survey_group_id INTEGER, time TEXT, UNIQUE (survey_group_id) ON CONFLICT REPLACE);";

    private static final String[] DEFAULT_INSERTS = new String[] {
            "INSERT INTO preferences VALUES('survey.language','')",
            "INSERT INTO preferences VALUES('survey.languagespresent','')",
            "INSERT INTO preferences VALUES('user.storelast','false')",
            "INSERT INTO preferences VALUES('data.cellular.upload','0')",
            "INSERT INTO preferences VALUES('plot.default.mode','manual')",
            "INSERT INTO preferences VALUES('plot.interval','60000')",
            "INSERT INTO preferences VALUES('user.lastuser.id','')",
            "INSERT INTO preferences VALUES('location.sendbeacon','true')",
            "INSERT INTO preferences VALUES('survey.precachehelp','1')",
            "INSERT INTO preferences VALUES('backend.server','')",
            "INSERT INTO preferences VALUES('screen.keepon','true')",
            "INSERT INTO preferences VALUES('precache.points.countries','2')",
            "INSERT INTO preferences VALUES('precache.points.limit','200')",
            "INSERT INTO preferences VALUES('survey.textsize','LARGE')",
            "INSERT INTO preferences VALUES('survey.checkforupdates','0')",
            "INSERT INTO preferences VALUES('remoteexception.upload','0')",
            "INSERT INTO preferences VALUES('survey.media.photo.shrink','true')",
            "INSERT INTO preferences VALUES('survey.media.photo.sizereminder','true')"
    };

    private static final String DATABASE_NAME = "surveydata";

    private static final String RESPONSE_JOIN = "survey_respondent LEFT OUTER JOIN survey_response ON (survey_respondent._id = survey_response.survey_respondent_id) LEFT OUTER JOIN user ON (user._id = survey_respondent.user_id)";
    private static final String PLOT_JOIN = "plot LEFT OUTER JOIN plot_point ON (plot._id = plot_point.plot_id) LEFT OUTER JOIN user ON (user._id = plot.user_id)";
    private static final String RESPONDENT_JOIN = "survey_respondent LEFT OUTER JOIN survey ON (survey_respondent.survey_id = survey._id)";

    private static final int VER_LAUNCH = 75;// FLOW version <= 1.12.0
    private static final int VER_TIME_TRACK = 76;
    private static final int VER_POINT_UPDATES = 77;
    private static final int DATABASE_VERSION = VER_POINT_UPDATES;

    private final Context context;

    /**
     * Helper class for creating the database tables and loading reference data
     * It is declared with package scope for VM optimizations
     * 
     * @author Christopher Fagiani
     */
    static class DatabaseHelper extends SQLiteOpenHelper {
        private static SQLiteDatabase database;
        private static volatile Object LOCK_OBJ = new Object();
        private volatile static int instanceCount = 0;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(USER_TABLE_CREATE);
            db.execSQL(SURVEY_TABLE_CREATE);
            db.execSQL(SURVEY_RESPONDENT_CREATE);
            db.execSQL(SURVEY_RESPONSE_CREATE);
            db.execSQL(PLOT_TABLE_CREATE);
            db.execSQL(PLOT_POINT_TABLE_CREATE);
            db.execSQL(PREFERENCES_TABLE_CREATE);
            db.execSQL(POINT_OF_INTEREST_TABLE_CREATE);
            db.execSQL(TRANSMISSION_HISTORY_TABLE_CREATE);
            db.execSQL(SURVEY_GROUP_TABLE_CREATE);
            db.execSQL(SURVEYED_LOCALE_TABLE_CREATE);
            db.execSQL(SYNC_TIME_TABLE_CREATE);
            createIndexes(db);
            for (int i = 0; i < DEFAULT_INSERTS.length; i++) {
                db.execSQL(DEFAULT_INSERTS[i]);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion);
            
            int version = oldVersion;
            
            // Apply database updates sequentially. It starts in the current 
            // version, hooking into the correspondent case block, and falls 
            // through to any future upgrade. If no break statement is found,
            // the upgrade will end up in the current version.
            switch (version) {
                case VER_LAUNCH:
                    // changes in version 76 - Time track
                    db.execSQL("ALTER TABLE survey_respondent ADD COLUMN survey_start INTEGER");
                    version = VER_TIME_TRACK;
                case VER_TIME_TRACK:
                    // changes in version 77 - Point Updates
                    db.execSQL("ALTER TABLE survey ADD COLUMN survey_group_id INTEGER");
                    db.execSQL("ALTER TABLE survey_respondent ADD COLUMN surveyed_locale_id TEXT");
                    db.execSQL(SURVEY_GROUP_TABLE_CREATE);
                    db.execSQL(SURVEYED_LOCALE_TABLE_CREATE);
                    db.execSQL(SYNC_TIME_TABLE_CREATE);
                    createIndexes(db);
                    version = VER_POINT_UPDATES;
            }

            if (version != DATABASE_VERSION) {
                Log.d(TAG, "onUpgrade() - Recreating the Database.");
                
                db.execSQL("DROP TABLE IF EXISTS " + Tables.RESPONSE);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.RESPONDENT);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEY);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.PLOT_POINT);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.PLOT);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.USER);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.PREFERENCES);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.POINT_OF_INTEREST);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.TRANSMISSION_HISTORY);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEY_GROUP);
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEYED_LOCALE);
                
                onCreate(db);
            }
        }

        @Override
        public SQLiteDatabase getWritableDatabase() {
            synchronized (LOCK_OBJ) {

                if (database == null || !database.isOpen()) {
                    database = super.getWritableDatabase();
                    instanceCount = 0;
                }
                instanceCount++;
                return database;
            }
        }

        @Override
        public void close() {
            synchronized (LOCK_OBJ) {
                instanceCount--;
                if (instanceCount <= 0) {
                    // close the database held by the helper (if any)
                    super.close();
                    if (database != null && database.isOpen()) {
                        // we may be holding a different database than the
                        // helper so
                        // close that too if it's still open.
                        database.close();
                    }
                    database = null;
                }
            }
        }
        
        private void createIndexes(SQLiteDatabase db) {
            // Included in point updates
            db.execSQL("CREATE INDEX respondent_uuid_idx ON " + Tables.RESPONDENT + "(uuid)");
            db.execSQL("CREATE INDEX response_idx ON " + Tables.RESPONSE + "(survey_respondent_id, question_id)");
            db.execSQL("CREATE INDEX locale_name_idx ON " + Tables.SURVEYED_LOCALE + "(name)");
            db.execSQL("CREATE INDEX submitted_date_idx ON " + Tables.RESPONDENT + "(submitted_date)");
        }

        /**
         * returns the value of a single setting identified by the key passed in
         */
        public String findPreference(SQLiteDatabase db, String key) {
            String value = null;
            Cursor cursor = db.query(Tables.PREFERENCES, new String[] {
                    KEY_COL,
                    VALUE_COL
            }, KEY_COL + " = ?", new String[] {
                key
            }, null,
                    null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    value = cursor.getString(cursor
                            .getColumnIndexOrThrow(VALUE_COL));
                }
                cursor.close();
            }
            return value;
        }

        /**
         * persists setting to the db
         * 
         * @param surveyId
         */
        public void savePreference(SQLiteDatabase db, String key, String value) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(VALUE_COL, value);
            int updated = db.update(Tables.PREFERENCES, updatedValues, KEY_COL
                    + " = ?", new String[] {
                key
            });
            if (updated <= 0) {
                updatedValues.put(KEY_COL, key);
                db.insert(Tables.PREFERENCES, null, updatedValues);
            }
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public SurveyDbAdapter(Context ctx) {
        this.context = ctx;
    }

    /**
     * Open or create the db
     * 
     * @throws SQLException if the database could be neither opened or created
     */
    public SurveyDbAdapter open() throws SQLException {
        databaseHelper = new DatabaseHelper(context);
        database = databaseHelper.getWritableDatabase();
        return this;
    }

    /**
     * close the db
     */
    public void close() {
        databaseHelper.close();
    }

    /**
     * Create a new survey using the title and body provided. If the survey is
     * successfully created return the new id, otherwise return a -1 to indicate
     * failure.
     * 
     * @param name survey name
     * @return rowId or -1 if failed
     */
    public long createSurvey(String name) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(DISP_NAME_COL, name);
        initialValues.put(HELP_DOWNLOADED_COL, "N");
        return database.insert(Tables.SURVEY, null, initialValues);
    }

    /**
     * returns a cursor that lists all unsent (sentFlag = false) survey data
     * 
     * @return
     */
    public Cursor fetchUnsentData() {
        Cursor cursor = database.query(RESPONSE_JOIN, new String[] {
                Tables.RESPONDENT + "." + PK_ID_COL, RESP_ID_COL, ANSWER_COL,
                ANSWER_TYPE_COL, QUESTION_FK_COL, DISP_NAME_COL, EMAIL_COL,
                DELIVERED_DATE_COL, SUBMITTED_DATE_COL,
                Tables.RESPONDENT + "." + SURVEY_FK_COL, SCORED_VAL_COL,
                STRENGTH_COL, UUID_COL, SURVEY_START_COL, SURVEYED_LOCALE_ID_COL
        }, SUBMITTED_FLAG_COL + "= 'true' AND "
                + INCLUDE_FLAG_COL + "='true' AND" + "(" + DELIVERED_DATE_COL
                + " is null OR " + MEDIA_SENT_COL + " <> 'true')", null, null,
                null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * returns a cursor that lists all unexported (sentFlag = false) survey data
     * 
     * @return
     */
    public Cursor fetchUnexportedData() {
        Cursor cursor = database.query(RESPONSE_JOIN, new String[] {
                Tables.RESPONDENT + "." + PK_ID_COL, RESP_ID_COL, ANSWER_COL,
                ANSWER_TYPE_COL, QUESTION_FK_COL, DISP_NAME_COL, EMAIL_COL,
                DELIVERED_DATE_COL, SUBMITTED_DATE_COL,
                Tables.RESPONDENT + "." + SURVEY_FK_COL, SCORED_VAL_COL,
                STRENGTH_COL, UUID_COL, SURVEY_START_COL, SURVEYED_LOCALE_ID_COL
        }, SUBMITTED_FLAG_COL + "= 'true' AND "
                + INCLUDE_FLAG_COL + "='true' AND " + EXPORTED_FLAG_COL
                + " <> 'true' AND " + "(" + DELIVERED_DATE_COL + " is null OR "
                + MEDIA_SENT_COL + " <> 'true')", null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * Get the amount of responses that have not been sent to the backend
     */
    public int unsentDataCount() {
        return fetchUnsentData().getCount();
    }

    /**
     * Get the amount of responses that have not been exported to the sd-card
     */
    public int unexportedDataCount() {
        return fetchUnexportedData().getCount();
    }

    /**
     * marks the data as submitted in the respondent table (submittedFlag =
     * true) thereby making it ready for transmission
     * 
     * @param respondentId
     */
    public void submitResponses(String respondentId) {
        ContentValues vals = new ContentValues();
        vals.put(SUBMITTED_FLAG_COL, "true");
        vals.put(SUBMITTED_DATE_COL, System.currentTimeMillis());
        vals.put(STATUS_COL, ConstantUtil.SUBMITTED_STATUS);
        database.update(Tables.RESPONDENT, vals,
                PK_ID_COL + "= " + respondentId, null);
    }

    /**
     * updates the respondent table by recording the sent date stamp
     * 
     * @param idList
     */
    public void markDataAsSent(Set<String> idList, String mediaSentFlag) {
        if (idList != null) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(DELIVERED_DATE_COL, System.currentTimeMillis()
                    + "");
            updatedValues.put(MEDIA_SENT_COL, mediaSentFlag);
            // enhanced FOR ok here since we're dealing with an implicit
            // iterator anyway
            for (String id : idList) {
                if (database.update(Tables.RESPONDENT, updatedValues, PK_ID_COL
                        + " = ?", new String[] {
                    id
                }) < 1) {
                    Log.e(TAG,
                            "Could not update record for Survey_respondent_id "
                                    + id);
                }
            }
        }
    }

    /**
     * updates the respondent table by recording the sent date stamp
     * 
     * @param idList
     */
    public void markDataAsExported(Set<String> idList) {
        if (idList != null && idList.size() > 0) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(EXPORTED_FLAG_COL, "true");
            // enhanced FOR ok here since we're dealing with an implicit
            // iterator anyway
            for (String id : idList) {
                if (database.update(Tables.RESPONDENT, updatedValues, PK_ID_COL
                        + " = ?", new String[] {
                    id
                }) < 1) {
                    Log.e(TAG, "Could not update record for Survey_respondent_id " + id);
                }
            }
        }
    }

    /**
     * updates the status of a survey response to the string passed in
     * 
     * @param surveyRespondentId
     * @param status
     */
    public void updateSurveyStatus(String surveyRespondentId, String status) {
        if (surveyRespondentId != null) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(STATUS_COL, status);
            updatedValues.put(SAVED_DATE_COL, System.currentTimeMillis());
            if (database.update(Tables.RESPONDENT, updatedValues, PK_ID_COL
                    + " = ?", new String[] {
                surveyRespondentId
            }) < 1) {
                Log.e(TAG, "Could not update status for Survey_respondent_id "
                        + surveyRespondentId);
            }

        }

    }

    /**
     * returns a cursor listing all users
     * 
     * @return
     */
    public Cursor listUsers() {
        Cursor cursor = database.query(Tables.USER, new String[] {
                PK_ID_COL,
                DISP_NAME_COL, EMAIL_COL
        }, DELETED_COL + " <> ?",
                new String[] {
                    ConstantUtil.IS_DELETED
                }, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * retrieves a user by ID
     * 
     * @param id
     * @return
     */
    public Cursor findUser(Long id) {
        Cursor cursor = database.query(Tables.USER, new String[] {
                PK_ID_COL,
                DISP_NAME_COL, EMAIL_COL
        }, PK_ID_COL + "=?",
                new String[] {
                    id.toString()
                }, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * if the ID is populated, this will update a user record. Otherwise, it
     * will be inserted
     * 
     * @param id
     * @param name
     * @param email
     * @return
     */
    public long createOrUpdateUser(Long id, String name, String email) {
        ContentValues initialValues = new ContentValues();
        Long idVal = id;
        initialValues.put(DISP_NAME_COL, name);
        initialValues.put(EMAIL_COL, email);
        initialValues.put(DELETED_COL, ConstantUtil.NOT_DELETED);

        if (idVal == null) {
            idVal = database.insert(Tables.USER, null, initialValues);
        } else {
            if (database.update(Tables.USER, initialValues, PK_ID_COL + "=?",
                    new String[] {
                        idVal.toString()
                    }) > 0) {
            }
        }
        return idVal;
    }

    /**
     * Return a Cursor over the list of all responses for a particular survey
     * respondent
     * 
     * @return Cursor over all responses
     */
    public Cursor fetchResponsesByRespondent(String respondentID) {
        return database.query(Tables.RESPONSE, new String[] {
                RESP_ID_COL,
                QUESTION_FK_COL, ANSWER_COL, ANSWER_TYPE_COL,
                SURVEY_RESPONDENT_ID_COL, INCLUDE_FLAG_COL, SCORED_VAL_COL,
                STRENGTH_COL
        }, SURVEY_RESPONDENT_ID_COL + "=?",
                new String[] {
                    respondentID
                }, null, null, null);
    }

    /**
     * loads a single question response
     * 
     * @param respondentId
     * @param questionId
     * @return
     */
    public QuestionResponse findSingleResponse(Long respondentId,
            String questionId) {
        QuestionResponse resp = null;
        Cursor cursor = database.query(Tables.RESPONSE, new String[] {
                RESP_ID_COL, QUESTION_FK_COL, ANSWER_COL, ANSWER_TYPE_COL,
                SURVEY_RESPONDENT_ID_COL, INCLUDE_FLAG_COL, SCORED_VAL_COL,
                STRENGTH_COL
        }, SURVEY_RESPONDENT_ID_COL + "=? and "
                + QUESTION_FK_COL + "=?",
                new String[] {
                        respondentId.toString(), questionId
                }, null,
                null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                resp = new QuestionResponse();
                resp.setQuestionId(questionId);
                resp.setRespondentId(respondentId);
                resp.setType(cursor.getString(cursor
                        .getColumnIndexOrThrow(ANSWER_TYPE_COL)));
                resp.setValue(cursor.getString(cursor
                        .getColumnIndexOrThrow(ANSWER_COL)));
                resp.setId(cursor.getLong(cursor
                        .getColumnIndexOrThrow(RESP_ID_COL)));
                resp.setIncludeFlag(cursor.getString(cursor
                        .getColumnIndexOrThrow(INCLUDE_FLAG_COL)));
                resp.setScoredValue(cursor.getString(cursor
                        .getColumnIndexOrThrow(SCORED_VAL_COL)));
                resp.setStrength(cursor.getString(cursor
                        .getColumnIndexOrThrow(STRENGTH_COL)));
            }
            cursor.close();
        }
        return resp;
    }

    /**
     * inserts or updates a question response after first looking to see if it
     * already exists in the database.
     * 
     * @param resp
     * @return
     */
    public QuestionResponse createOrUpdateSurveyResponse(QuestionResponse resp) {
        QuestionResponse responseToSave = findSingleResponse(
                resp.getRespondentId(), resp.getQuestionId());
        if (responseToSave != null) {
            responseToSave.setValue(resp.getValue());
            responseToSave.setStrength(resp.getStrength());
            responseToSave.setScoredValue(resp.getScoredValue());
            if (resp.getType() != null) {
                responseToSave.setType(resp.getType());
            }
        } else {
            responseToSave = resp;
        }
        long id = -1;
        ContentValues initialValues = new ContentValues();
        initialValues.put(ANSWER_COL, responseToSave.getValue());
        initialValues.put(ANSWER_TYPE_COL, responseToSave.getType());
        initialValues.put(QUESTION_FK_COL, responseToSave.getQuestionId());
        initialValues.put(SURVEY_RESPONDENT_ID_COL,
                responseToSave.getRespondentId());
        initialValues.put(SCORED_VAL_COL, responseToSave.getScoredValue());
        initialValues.put(INCLUDE_FLAG_COL, resp.getIncludeFlag());
        initialValues.put(STRENGTH_COL, responseToSave.getStrength());
        if (responseToSave.getId() == null) {
            id = database.insert(Tables.RESPONSE, null, initialValues);
        } else {
            if (database.update(Tables.RESPONSE, initialValues, RESP_ID_COL
                    + "=?", new String[] {
                responseToSave.getId().toString()
            }) > 0) {
                id = responseToSave.getId();
            }
        }
        responseToSave.setId(id);
        resp.setId(id);
        return responseToSave;
    }

    /**
     * this method will get the max survey respondent ID that has an unsubmitted
     * survey or, if none exists, will create a new respondent
     * 
     * @param surveyId
     * @return
     */
    public long createOrLoadSurveyRespondent(String surveyId, String userId, long surveyGroupId, String surveyedLocaleId) {
        String where = SUBMITTED_FLAG_COL + "='false' and "
                + SURVEY_FK_COL + "=? and " + STATUS_COL + " =?";
        List<String> argList =  new ArrayList<String>();
        argList.add(surveyId);
        argList.add(ConstantUtil.CURRENT_STATUS);
        
        if (surveyedLocaleId != null) {
            where += " and " + SURVEYED_LOCALE_ID_COL + " =?";
            argList.add(surveyedLocaleId);
        }
        
        Cursor results = database.query(Tables.RESPONDENT, new String[] {
            "max(" + PK_ID_COL + ")" }, 
            where,
            argList.toArray(new String[argList.size()]),
            null, null, null);
        
        long id = -1;
        if (results != null && results.getCount() > 0) {
            results.moveToFirst();
            id = results.getLong(0);
        }
        if (results != null) {
            results.close();
        }
        if (id <= 0) {
            if (surveyedLocaleId == null) {
                surveyedLocaleId = createSurveyedLocale(surveyGroupId);
            }
            id = createSurveyRespondent(surveyId, userId, surveyedLocaleId);
        }
        return id;
    }

    /**
     * creates a new unsubmitted survey respondent record
     * 
     * @param surveyId
     * @return
     */
    public long createSurveyRespondent(String surveyId, String userId, String surveyedLocaleId) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(SURVEY_FK_COL, surveyId);
        initialValues.put(SUBMITTED_FLAG_COL, "false");
        initialValues.put(EXPORTED_FLAG_COL, "false");
        initialValues.put(USER_FK_COL, userId);
        initialValues.put(STATUS_COL, ConstantUtil.CURRENT_STATUS);
        initialValues.put(UUID_COL, UUID.randomUUID().toString());
        initialValues.put(SURVEY_START_COL, System.currentTimeMillis());
        initialValues.put(SURVEYED_LOCALE_ID_COL, surveyedLocaleId);
        return database.insert(Tables.RESPONDENT, null, initialValues);
    }

    /**
     * creates a new plot point in the database for the plot and coordinates
     * sent in
     * 
     * @param plotId
     * @param lat
     * @param lon
     * @return
     */
    public long savePlotPoint(String plotId, String lat, String lon,
            double currentElevation) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(PLOT_FK_COL, plotId);
        initialValues.put(LAT_COL, lat);
        initialValues.put(LON_COL, lon);
        initialValues.put(ELEVATION_COL, currentElevation);
        initialValues.put(CREATED_DATE_COL, System.currentTimeMillis());
        return database.insert(Tables.PLOT_POINT, null, initialValues);
    }

    /**
     * returns a cursor listing all plots with the status passed in or all plots
     * if status is null
     * 
     * @return
     */
    public Cursor listPlots(String status) {
        Cursor cursor = database.query(Tables.PLOT, new String[] {
                PK_ID_COL,
                DISP_NAME_COL, DESC_COL, CREATED_DATE_COL, STATUS_COL
        },
                status == null ? null : STATUS_COL + " = ?",
                status == null ? null : new String[] {
                    status
                }, null, null,
                null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * retrieves a plot by ID
     * 
     * @param id
     * @return
     */
    public Cursor findPlot(Long id) {
        Cursor cursor = database.query(Tables.PLOT, new String[] {
                PK_ID_COL,
                DISP_NAME_COL, DESC_COL, CREATED_DATE_COL, STATUS_COL
        },
                PK_ID_COL + "=?", new String[] {
                    id.toString()
                }, null, null,
                null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * if the ID is populated, this will update a plot record. Otherwise, it
     * will be inserted
     * 
     * @param id
     * @param name
     * @param email
     * @return
     */
    public long createOrUpdatePlot(Long id, String name, String desc,
            String userId) {
        ContentValues initialValues = new ContentValues();
        Long idVal = id;
        initialValues.put(DISP_NAME_COL, name);
        initialValues.put(DESC_COL, desc);
        initialValues.put(CREATED_DATE_COL, System.currentTimeMillis());
        initialValues.put(USER_FK_COL, userId);
        initialValues.put(STATUS_COL, ConstantUtil.IN_PROGRESS_STATUS);

        if (idVal == null) {
            idVal = database.insert(Tables.PLOT, null, initialValues);
        } else {
            if (database.update(Tables.PLOT, initialValues, PK_ID_COL + "=?",
                    new String[] {
                        idVal.toString()
                    }) > 0) {
            }
        }
        return idVal;
    }

    /**
     * retrieves all the points for a given plot
     * 
     * @param plotId
     * @return
     */
    public Cursor listPlotPoints(String plotId, String afterTime) {
        Cursor cursor = database
                .query(Tables.PLOT_POINT, new String[] {
                        PK_ID_COL, LAT_COL,
                        LON_COL, CREATED_DATE_COL
                }, PLOT_FK_COL + " = ? and "
                        + CREATED_DATE_COL + " > ?", new String[] {
                        plotId,
                        afterTime != null ? afterTime : "0"
                }, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * updates the status of a plot in the db
     * 
     * @param plotId
     * @param status
     */
    public long updatePlotStatus(String plotId, String status) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(STATUS_COL, status);
        return database.update(Tables.PLOT, initialValues, PK_ID_COL + " = ?",
                new String[] {
                    plotId
                });
    }

    /**
     * updates the status of all the plots identified by the ids sent in to the
     * value of status
     * 
     * @param idList
     * @param status
     */
    public void updatePlotStatus(Set<String> idList, String status) {
        if (idList != null) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(STATUS_COL, status);
            // enhanced FOR ok here since we're dealing with an implicit
            // iterator anyway
            for (String id : idList) {
                if (updatePlotStatus(id, status) < 1) {
                    Log.e(TAG, "Could not update plot status for plot " + id);
                }
            }
        }
    }

    /**
     * lists all plot points for plots that are in the COMPLETED state
     * 
     * @return
     */
    public Cursor listCompletePlotPoints() {
        Cursor cursor = database
                .query(PLOT_JOIN, new String[] {
                        Tables.PLOT + "." + PK_ID_COL + " as plot_id",
                        Tables.PLOT + "." + DISP_NAME_COL,
                        Tables.PLOT_POINT + "." + PK_ID_COL, LAT_COL, LON_COL,
                        ELEVATION_COL,
                        Tables.PLOT_POINT + "." + CREATED_DATE_COL
                }, STATUS_COL
                        + "= ?", new String[] {
                    ConstantUtil.COMPLETE_STATUS
                },
                        null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * deletes the plot_point row denoted by the ID passed in
     * 
     * @param id
     */
    public void deletePlotPoint(String id) {
        database.delete(Tables.PLOT_POINT, PK_ID_COL + " = ?",
                new String[] {
                    id
                });
    }

    /**
     * returns a list of survey objects that are out of date (missing from the
     * db or with a lower version number). If a survey is present but marked as
     * deleted, it will not be listed as out of date (and thus won't be updated)
     * 
     * @param surveys
     * @return
     */
    public List<Survey> checkSurveyVersions(List<Survey> surveys) {
        List<Survey> outOfDateSurveys = new ArrayList<Survey>();
        for (int i = 0; i < surveys.size(); i++) {
            Cursor cursor = database.query(Tables.SURVEY,
                    new String[] {
                        PK_ID_COL
                    },
                    PK_ID_COL + " = ? and (" + VERSION_COL + " >= ? or "
                            + DELETED_COL + " = ?)", new String[] {
                            surveys.get(i).getId(),
                            surveys.get(i).getVersion() + "",
                            ConstantUtil.IS_DELETED
                    }, null, null, null);

            if (cursor == null || cursor.getCount() <= 0) {
                outOfDateSurveys.add(surveys.get(i));
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return outOfDateSurveys;
    }

    /**
     * updates the survey table by recording the help download flag
     * 
     * @param idList
     */
    public void markSurveyHelpDownloaded(String surveyId, boolean isDownloaded) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(HELP_DOWNLOADED_COL, isDownloaded ? "Y" : "N");

        if (database.update(Tables.SURVEY, updatedValues, PK_ID_COL + " = ?",
                new String[] {
                    surveyId
                }) < 1) {
            Log.e(TAG, "Could not update record for Survey " + surveyId);
        }
    }

    /**
     * updates a survey in the db and resets the deleted flag to "N"
     * 
     * @param survey
     * @return
     */
    public void saveSurvey(Survey survey) {
        Cursor cursor = database.query(Tables.SURVEY,
                new String[] {
                    PK_ID_COL
                }, PK_ID_COL + " = ?",
                new String[] {
                    survey.getId(),
                }, null, null, null);
        final long surveyGroupId = survey.getSurveyGroup() != null ? 
                survey.getSurveyGroup().getId() 
                : SurveyGroup.ID_NONE;
        
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(PK_ID_COL, survey.getId());
        updatedValues.put(VERSION_COL, survey.getVersion());
        updatedValues.put(TYPE_COL, survey.getType());
        updatedValues.put(LOCATION_COL, survey.getLocation());
        updatedValues.put(FILENAME_COL, survey.getFileName());
        updatedValues.put(DISP_NAME_COL, survey.getName());
        updatedValues.put(LANGUAGE_COL, survey.getLanguage() != null ? survey
                .getLanguage().toLowerCase() : ConstantUtil.ENGLISH_CODE);
        updatedValues.put(SURVEY_GROUP_ID_COL, surveyGroupId);
        updatedValues.put(HELP_DOWNLOADED_COL, survey.isHelpDownloaded() ? "Y"
                : "N");
        updatedValues.put(DELETED_COL, ConstantUtil.NOT_DELETED);

        if (cursor != null && cursor.getCount() > 0) {
            // if we found an item, it's an update, otherwise, it's an insert
            database.update(Tables.SURVEY, updatedValues, PK_ID_COL + " = ?",
                    new String[] {
                        survey.getId()
                    });
        } else {
            database.insert(Tables.SURVEY, null, updatedValues);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Gets a single survey from the db using its primary key
     */
    public Survey findSurvey(String surveyId) {
        Survey survey = null;
        Cursor cursor = database.query(Tables.SURVEY, new String[] {
                PK_ID_COL,
                DISP_NAME_COL, LOCATION_COL, FILENAME_COL, TYPE_COL,
                LANGUAGE_COL, HELP_DOWNLOADED_COL
        }, PK_ID_COL + " = ?",
                new String[] {
                    surveyId
                }, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                survey = new Survey();
                survey.setId(surveyId);
                survey.setName(cursor.getString(cursor
                        .getColumnIndexOrThrow(DISP_NAME_COL)));
                survey.setLocation(cursor.getString(cursor
                        .getColumnIndexOrThrow(LOCATION_COL)));
                survey.setFileName(cursor.getString(cursor
                        .getColumnIndexOrThrow(FILENAME_COL)));
                survey.setType(cursor.getString(cursor
                        .getColumnIndexOrThrow(TYPE_COL)));
                survey.setHelpDownloaded(cursor.getString(cursor
                        .getColumnIndexOrThrow(HELP_DOWNLOADED_COL)));
                survey.setLanguage(cursor.getString(cursor
                        .getColumnIndexOrThrow(LANGUAGE_COL)));
            }
            cursor.close();
        }

        return survey;
    }

    /**
     * lists all survey respondents with specified status sorted by creation
     * order (primary key) or delivered date
     * 
     * @param status
     * @return
     */
    public Cursor listSurveyRespondent(String status, boolean byDelivered) {
        String[] whereParams = {
            status
        };
        String sortBy;
        if (byDelivered) {
            sortBy = "case when " + DELIVERED_DATE_COL
                    + " is null then 0 else 1 end, " + DELIVERED_DATE_COL
                    + " desc";
        } else {
            sortBy = Tables.RESPONDENT + "." + PK_ID_COL + " desc";
        }
        Cursor cursor = database.query(RESPONDENT_JOIN, new String[] {
                Tables.RESPONDENT + "." + PK_ID_COL, DISP_NAME_COL,
                SAVED_DATE_COL, SURVEY_FK_COL, USER_FK_COL, SUBMITTED_DATE_COL,
                DELIVERED_DATE_COL, UUID_COL
        }, "status = ?", whereParams,
                null, null, sortBy);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * count survey respondents by status
     * 
     * @param status
     * @return
     */
    public int countSurveyRespondents(String status) {
        String[] whereParams = {
            status
        };
        int i = 0;
        Cursor cursor = database.rawQuery(
                "SELECT COUNT(*) as theCount FROM survey_respondent WHERE status = ?",
                whereParams);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                i = cursor.getInt(0);
            }
            cursor.close();
        }
        return i;
    }

    /**
     * Lists all non-deleted surveys from the database
     */
    public ArrayList<Survey> listSurveys(long surveyGroupId) {
        ArrayList<Survey> surveys = new ArrayList<Survey>();
        String whereClause = DELETED_COL + " <> ?";
        String[] whereParams = null;
        if (surveyGroupId > 0) {
            whereClause += " and " + SURVEY_GROUP_ID_COL + " = ?";
            whereParams = new String[] {
                    ConstantUtil.IS_DELETED,
                    String.valueOf(surveyGroupId)
            };
        } else {
            whereParams = new String[] {
                ConstantUtil.IS_DELETED
            };
        }
        Cursor cursor = database.query(Tables.SURVEY, new String[] {
                PK_ID_COL,
                DISP_NAME_COL, LOCATION_COL, FILENAME_COL, TYPE_COL,
                LANGUAGE_COL, HELP_DOWNLOADED_COL, VERSION_COL
        }, whereClause,
                whereParams, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    Survey survey = new Survey();
                    survey.setId(cursor.getString(cursor
                            .getColumnIndexOrThrow(PK_ID_COL)));
                    survey.setName(cursor.getString(cursor
                            .getColumnIndexOrThrow(DISP_NAME_COL)));
                    survey.setLocation(cursor.getString(cursor
                            .getColumnIndexOrThrow(LOCATION_COL)));
                    survey.setFileName(cursor.getString(cursor
                            .getColumnIndexOrThrow(FILENAME_COL)));
                    survey.setType(cursor.getString(cursor
                            .getColumnIndexOrThrow(TYPE_COL)));
                    survey.setHelpDownloaded(cursor.getString(cursor
                            .getColumnIndexOrThrow(HELP_DOWNLOADED_COL)));
                    survey.setLanguage(cursor.getString(cursor
                            .getColumnIndexOrThrow(LANGUAGE_COL)));
                    survey.setVersion(cursor.getDouble(cursor
                            .getColumnIndexOrThrow(VERSION_COL)));
                    surveys.add(survey);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return surveys;
    }

    /**
     * marks a survey record identified by the ID passed in as deleted.
     * 
     * @param surveyId
     */
    public void deleteSurvey(String surveyId, boolean physicalDelete) {
        if (!physicalDelete) {
            ContentValues updatedValues = new ContentValues();
            updatedValues.put(DELETED_COL, ConstantUtil.IS_DELETED);
            database.update(Tables.SURVEY, updatedValues, PK_ID_COL + " = ?",
                    new String[] {
                        surveyId
                    });
        } else {
            database.delete(Tables.SURVEY, PK_ID_COL + " = ? ",
                    new String[] {
                        surveyId
                    });
        }
    }

    /**
     * returns the value of a single setting identified by the key passed in
     */
    public String findPreference(String key) {
        return databaseHelper.findPreference(database, key);
    }

    /**
     * Lists all settings from the database
     */
    public HashMap<String, String> listPreferences() {
        HashMap<String, String> settings = new HashMap<String, String>();
        Cursor cursor = database.query(Tables.PREFERENCES, new String[] {
                KEY_COL, VALUE_COL
        }, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    settings.put(cursor.getString(cursor
                            .getColumnIndexOrThrow(KEY_COL)), cursor
                            .getString(cursor.getColumnIndexOrThrow(VALUE_COL)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return settings;
    }

    /**
     * persists setting to the db
     * 
     * @param surveyId
     */
    public void savePreference(String key, String value) {
        databaseHelper.savePreference(database, key, value);
    }

    /**
     * deletes all the surveys from the database
     */
    public void deleteAllSurveys() {
        database.delete(Tables.SURVEY, null, null);
    }

    /**
     * deletes all the points of interest
     */
    public void deleteAllPoints() {
        database.delete(Tables.POINT_OF_INTEREST, null, null);
    }

    /**
     * deletes all survey responses from the database
     */
    public void deleteAllResponses() {
        database.delete(Tables.RESPONSE, null, null);
        database.delete(Tables.RESPONDENT, null, null);
    }

    /**
     * deletes all survey responses from the database for a specific respondent
     */
    public void deleteResponses(String respondentId) {
        database.delete(Tables.RESPONSE, SURVEY_RESPONDENT_ID_COL + "=?",
                new String[] {
                    respondentId
                });
    }

    /**
     * deletes the respondent record and any responses it contains
     * 
     * @param respondentId
     */
    public void deleteRespondent(String respondentId) {
        deleteResponses(respondentId);
        database.delete(Tables.RESPONDENT, PK_ID_COL + "=?",
                new String[] {
                    respondentId
                });
    }

    /**
     * deletes a single response
     * 
     * @param respondentId
     * @param questionId
     */
    public void deleteResponse(String respondentId, String questionId) {
        database.delete(Tables.RESPONSE, SURVEY_RESPONDENT_ID_COL + "=? AND "
                + QUESTION_FK_COL + "=?", new String[] {
                respondentId,
                questionId
        });
    }


    /**
     * inserts a transmissionHistory row into the db
     * 
     * @param respId
     * @param fileName
     * @param status
     * @return uid of created record
     */
    public Long createTransmissionHistory(Long respId, String fileName,
            String status) {
        ContentValues initialValues = new ContentValues();
        Long idVal = null;
        initialValues.put(SURVEY_RESPONDENT_ID_COL, respId);
        initialValues.put(FILENAME_COL, fileName);

        if (status != null) {
            final long time = System.currentTimeMillis();
            initialValues.put(STATUS_COL, status);
            if (ConstantUtil.IN_PROGRESS_STATUS.equals(status)) {
                initialValues.put(TRANS_START_COL, time);
            } else if (ConstantUtil.DOWNLOADED_STATUS.equals(status)) {
                // Mark both columns with the same timestamp.
                initialValues.put(TRANS_START_COL, time);
                initialValues.put(DELIVERED_DATE_COL, time);
            }
        } else {
            initialValues.put(TRANS_START_COL, (Long) null);
            initialValues.put(STATUS_COL, ConstantUtil.QUEUED_STATUS);
        }
        idVal = database
                .insert(Tables.TRANSMISSION_HISTORY, null, initialValues);
        return idVal;
    }

    /**
     * updates the first matching transmission history record with the status
     * passed in. If the status == Completed, the completion date is updated. If
     * the status == In Progress, the start date is updated.
     * 
     * @param respondId
     * @param fileName
     * @param status
     */
    public void updateTransmissionHistory(Long respondId, String fileName,
            String status) {
        List<FileTransmission> transList = listFileTransmission(respondId,
                fileName, true);
        Long idVal = null;
        if (transList != null && transList.size() > 0) {
            idVal = transList.get(0).getId();
            if (idVal != null) {
                ContentValues vals = new ContentValues();
                vals.put(STATUS_COL, status);
                if (ConstantUtil.COMPLETE_STATUS.equals(status)) {
                    vals.put(DELIVERED_DATE_COL, System.currentTimeMillis()
                            + "");
                } else if (ConstantUtil.IN_PROGRESS_STATUS.equals(status)) {
                    vals.put(TRANS_START_COL, System.currentTimeMillis() + "");
                }
                database.update(Tables.TRANSMISSION_HISTORY, vals, PK_ID_COL
                        + " = ?", new String[] {
                    idVal.toString()
                });
            }
            else
                // it should have been found
                Log.e(TAG,
                        "Could not update transmission history record for respondent_id "
                                + respondId
                                + " filename "
                                + fileName);
        }
    }

    public void updateTransmissionHistory(Set<String> respondentIDs, String fileName,
            String status) {
        for (String id : respondentIDs) {
            updateTransmissionHistory(Long.valueOf(id), fileName, status);
        }
    }

    /**
     * lists all the file transmissions for the values passed in.
     * 
     * @param respondentId - MANDATORY id of the survey respondent
     * @param fileName - OPTIONAL file name
     * @param incompleteOnly - if true, only rows without a complete status will
     *            be returned
     * @return
     */
    public List<FileTransmission> listFileTransmission(Long respondentId,
            String fileName, boolean incompleteOnly) {
        List<FileTransmission> transList = null;

        String whereClause = SURVEY_RESPONDENT_ID_COL + "=?";
        if (incompleteOnly) {
            whereClause = whereClause + " AND " + STATUS_COL + " <> '"
                    + ConstantUtil.COMPLETE_STATUS + "'";
        }
        String[] whereValues = null;

        if (fileName != null && fileName.trim().length() > 0) {
            whereClause = whereClause + " AND " + FILENAME_COL + " = ?";
            whereValues = new String[2];
            whereValues[0] = respondentId.toString();
            whereValues[1] = fileName;

        } else {
            whereValues = new String[] {
                respondentId.toString()
            };
        }

        Cursor cursor = database.query(Tables.TRANSMISSION_HISTORY,
                new String[] {
                        PK_ID_COL, FILENAME_COL, STATUS_COL,
                        TRANS_START_COL, DELIVERED_DATE_COL,
                        SURVEY_RESPONDENT_ID_COL
                }, whereClause, whereValues,
                null, null, TRANS_START_COL + " desc");
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                transList = new ArrayList<FileTransmission>();
                do {
                    FileTransmission trans = new FileTransmission();
                    trans.setId(cursor.getLong(cursor
                            .getColumnIndexOrThrow(PK_ID_COL)));
                    trans.setRespondentId(respondentId);
                    trans.setFileName(cursor.getString(cursor
                            .getColumnIndexOrThrow(FILENAME_COL)));
                    Long startDateMillis = cursor.getLong(cursor
                            .getColumnIndexOrThrow(TRANS_START_COL));
                    if (startDateMillis != null && startDateMillis > 0) {
                        trans.setStartDate(new Date(startDateMillis));
                    }
                    Long delivDateMillis = cursor.getLong(cursor
                            .getColumnIndexOrThrow(DELIVERED_DATE_COL));
                    if (delivDateMillis != null && delivDateMillis > 0) {
                        trans.setEndDate(new Date(delivDateMillis));
                    }
                    trans.setStatus(cursor.getString(cursor
                            .getColumnIndexOrThrow(STATUS_COL)));
                    transList.add(trans);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return transList;
    }

    /**
     * marks submitted data as unsent. If an ID is passed in, only that
     * submission will be updated. If id is null, ALL data will be marked as
     * unsent.
     */
    public void markDataUnsent(Long respondentId) {
        executeSql("update survey_respondent set media_sent_flag = 'false', delivered_date = null where _id = "
                + respondentId);
    }
    
    public void markRecordUnsent(String recordId) {
        executeSql("update survey_respondent set media_sent_flag = 'false', delivered_date = null where surveyed_locale_id = '"
                + recordId + "'");
    }
    
    public void markSurveyGroupUnsent(long surveyGroupId) {
        executeSql("update survey_respondent set media_sent_flag = 'false', delivered_date = null where survey_id in "
                + "(select _id from survey where survey_group_id = " + surveyGroupId + ")");
    }

    /**
     * executes a single insert/update/delete DML or any DDL statement without
     * any bind arguments.
     * 
     * @param sql
     */
    public void executeSql(String sql) {
        database.execSQL(sql);
    }

    /**
     * reinserts the test survey into the database. For debugging purposes only.
     * The survey xml must exist in the APK
     */
    public void reinstallTestSurvey() {
        executeSql("insert into survey values(999991,'Sample Survey', 1.0,'Survey','res','testsurvey','english','N','N')");
    }

    /**
     * permanently deletes all surveys, responses, users and transmission
     * history from the database
     */
    public void clearAllData() {
        // User generated data
        clearCollectedData();

        // Surveys and preferences
        executeSql("delete from survey");
        executeSql("delete from user");
        executeSql("update preferences set value = '' where key = 'user.lastuser.id'");
    }

    /**
     * Permanently deletes user generated data from the database. It will clear
     * any response saved in the database, as well as the transmission history.
     */
    public void clearCollectedData() {
        executeSql("delete from survey_respondent");
        executeSql("delete from survey_response");
        executeSql("delete from transmission_history");
    }

    /**
     * performs a soft-delete on a user
     * 
     * @param id
     */
    public void deleteUser(Long id) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(DELETED_COL, "Y");
        database.update(Tables.USER, updatedValues, PK_ID_COL + " = ?",
                new String[] {
                    id.toString()
                });
    }

    public HashSet<String> stringToSet(String item) {
        HashSet<String> set = new HashSet<String>();
        StringTokenizer strTok = new StringTokenizer(item, ",");
        while (strTok.hasMoreTokens()) {
            set.add(strTok.nextToken());
        }
        return set;
    }

    public String setToString(HashSet<String> set) {
        boolean isFirst = true;
        StringBuffer buffer = new StringBuffer();
        Iterator<String> itr = set.iterator();

        for (int i = 0; i < set.size(); i++) {
            if (!isFirst) {
                buffer.append(",");
            } else {
                isFirst = false;
            }
            buffer.append(itr.next());
        }
        return buffer.toString();
    }

    public void addLanguages(String[] values) {
        // values holds the 2-letter codes of the languages. We first have to
        // find out what the indexes are

        String[] langCodesArray = context.getResources().getStringArray(
                R.array.alllanguagecodes);
        int[] valuesIndex = new int[values.length];
        List<String> langCodesList = Arrays.asList(langCodesArray);
        int index;
        for (int i = 0; i < values.length; i++) {
            index = langCodesList.indexOf(values[i]);
            if (index != -1) {
                valuesIndex[i] = index;
            }
        }

        String langsSelection = findPreference(ConstantUtil.SURVEY_LANG_SETTING_KEY);
        String langsPresentIndexes = findPreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY);

        HashSet<String> langsSelectionSet = stringToSet(langsSelection);
        HashSet<String> langsPresentIndexesSet = stringToSet(langsPresentIndexes);

        for (int i = 0; i < values.length; i++) {
            // values[0] holds the default language. That is the one that will
            // be turned 'on'.
            if (i == 0) {
                langsSelectionSet.add(valuesIndex[i] + "");
            }
            langsPresentIndexesSet.add(valuesIndex[i] + "");
        }

        String newLangsSelection = setToString(langsSelectionSet);
        String newLangsPresentIndexes = setToString(langsPresentIndexesSet);

        savePreference(ConstantUtil.SURVEY_LANG_SETTING_KEY, newLangsSelection);
        savePreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY,
                newLangsPresentIndexes);
    }
    
    public void addSurveyGroup(SurveyGroup surveyGroup) {
        ContentValues values = new ContentValues();
        values.put(SurveyGroupAttrs.ID, surveyGroup.getId());
        values.put(SurveyGroupAttrs.NAME, surveyGroup.getName());
        values.put(SurveyGroupAttrs.REGISTER_SURVEY_ID, surveyGroup.getRegisterSurveyId());
        values.put(SurveyGroupAttrs.MONITORED, surveyGroup.isMonitored() ? 1 : 0);
        database.insert(Tables.SURVEY_GROUP, null, values);
    }
    
    public static SurveyGroup getSurveyGroup(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(SurveyGroupAttrs.ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyGroupAttrs.NAME));
        String registerSurveyId = cursor.getString(cursor.getColumnIndexOrThrow(SurveyGroupAttrs.REGISTER_SURVEY_ID));
        boolean monitored = cursor.getInt(cursor.getColumnIndexOrThrow(SurveyGroupAttrs.MONITORED)) > 0;
        return new SurveyGroup(id, name, registerSurveyId, monitored);
    }
    
    public Cursor getSurveyGroups() {
        Cursor cursor = database.query(Tables.SURVEY_GROUP, 
                new String[] {SurveyGroupAttrs.ID, SurveyGroupAttrs.NAME, SurveyGroupAttrs.REGISTER_SURVEY_ID, SurveyGroupAttrs.MONITORED}, 
                null, null, null, null, null);
        
        return cursor;
    }
    
    public Cursor getSurveyGroup(long id) {
        String where = null;
        String[] selectionArgs = null;
        
        if (id != SurveyGroup.ID_NONE) {
            where = SurveyGroupAttrs.ID + "= ?";
            selectionArgs = new String[] {String.valueOf(id)};
        }
        
        Cursor cursor = database.query(Tables.SURVEY_GROUP, 
                new String[] {SurveyGroupAttrs.ID, SurveyGroupAttrs.NAME, SurveyGroupAttrs.REGISTER_SURVEY_ID, SurveyGroupAttrs.MONITORED}, 
                where, selectionArgs,
                null, null, null);
        
        return cursor;
    }
    
    public String createSurveyedLocale(long surveyGroupId) {
        String base32Id = Base32.base32Uuid();
        // Put dashes between the 4-5 and 8-9 positions to increase readability
        String id = base32Id.substring(0, 4) + "-" + base32Id.substring(4, 8) + "-" + base32Id.substring(8);
        String name = "Unknown";// TODO
        double lat = 0.0d;// TODO
        double lon = 0.0d;// TODO
        ContentValues values = new ContentValues();
        values.put(SurveyedLocaleAttrs.SURVEYED_LOCALE_ID, id);
        values.put(SurveyedLocaleAttrs.SURVEY_GROUP_ID, surveyGroupId);
        values.put(SurveyedLocaleAttrs.NAME, name);
        values.put(SurveyedLocaleAttrs.LATITUDE, lat);
        values.put(SurveyedLocaleAttrs.LONGITUDE, lon);
        database.insert(Tables.SURVEYED_LOCALE, null, values);
        
        return id;
    }
    
    public static SurveyedLocale getSurveyedLocale(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(SurveyedLocaleAttrs.SURVEYED_LOCALE_ID));
        long surveyGroupId = cursor.getLong(cursor.getColumnIndexOrThrow(SurveyedLocaleAttrs.SURVEY_GROUP_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(SurveyedLocaleAttrs.NAME));
        double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(SurveyedLocaleAttrs.LATITUDE));
        double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(SurveyedLocaleAttrs.LONGITUDE));
        return new SurveyedLocale(id, name, surveyGroupId, latitude, longitude);
    }
    
    public Cursor getSurveyedLocales(long surveyGroupId) {
        Cursor cursor = database.query(Tables.SURVEYED_LOCALE, 
                new String[] {SurveyedLocaleAttrs.ID, SurveyedLocaleAttrs.SURVEYED_LOCALE_ID, SurveyedLocaleAttrs.SURVEY_GROUP_ID,
                        SurveyedLocaleAttrs.NAME, SurveyedLocaleAttrs.LATITUDE, SurveyedLocaleAttrs.LONGITUDE},
                SurveyedLocaleAttrs.SURVEY_GROUP_ID + " = ?",
                new String[] {String.valueOf(surveyGroupId)},
                null, null, null);
        
        return cursor;
    }
    
    public SurveyedLocale getSurveyedLocale(String surveyedLocaleId) {
        Cursor cursor = database.query(Tables.SURVEYED_LOCALE, 
                new String[] {SurveyedLocaleAttrs.ID, SurveyedLocaleAttrs.SURVEYED_LOCALE_ID, SurveyedLocaleAttrs.SURVEY_GROUP_ID,
                        SurveyedLocaleAttrs.NAME, SurveyedLocaleAttrs.LATITUDE, SurveyedLocaleAttrs.LONGITUDE},
                SurveyedLocaleAttrs.SURVEYED_LOCALE_ID + " = ?",
                new String[] {String.valueOf(surveyedLocaleId)},
                null, null, null);
        
        SurveyedLocale locale = null;
        if (cursor.moveToFirst()) {
            locale = getSurveyedLocale(cursor);
        }
        cursor.close();
        
        return locale;
    }
    
    public static Survey getSurvey(Cursor cursor) {
        Survey survey = new Survey();
        survey.setId(cursor.getString(cursor.getColumnIndexOrThrow(PK_ID_COL)));
        survey.setName(cursor.getString(cursor.getColumnIndexOrThrow(DISP_NAME_COL)));
        survey.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(LOCATION_COL)));
        survey.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(FILENAME_COL)));
        survey.setType(cursor.getString(cursor.getColumnIndexOrThrow(TYPE_COL)));
        survey.setHelpDownloaded(cursor.getString(cursor.getColumnIndexOrThrow(HELP_DOWNLOADED_COL)));
        survey.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(LANGUAGE_COL)));
        survey.setVersion(cursor.getDouble(cursor.getColumnIndexOrThrow(VERSION_COL)));
        return survey;
    }

    public Cursor getSurveys(long surveyGroupId) {
        String whereClause = DELETED_COL + " <> ?";
        String[] whereParams = null;
        if (surveyGroupId > 0) {
            whereClause += " and " + SURVEY_GROUP_ID_COL + " = ?";
            whereParams = new String[] {
                    ConstantUtil.IS_DELETED,
                    String.valueOf(surveyGroupId)
            };
        } else {
            whereParams = new String[] {
                ConstantUtil.IS_DELETED
            };
        }
        
        return database.query(Tables.SURVEY, new String[] {
                PK_ID_COL,
                DISP_NAME_COL, LOCATION_COL, FILENAME_COL, TYPE_COL,
                LANGUAGE_COL, HELP_DOWNLOADED_COL, VERSION_COL},
                whereClause, whereParams, null, null, null);
    }
    
    public Cursor getSurveyInstances(long surveyGroupId) {
        final String sortBy = 
            "case when " + DELIVERED_DATE_COL
                    + " is null then 0 else 1 end, " + DELIVERED_DATE_COL
                    + " desc";
        Cursor cursor = database.query(RESPONDENT_JOIN, new String[] {
                Tables.RESPONDENT + "." + PK_ID_COL, DISP_NAME_COL,
                SAVED_DATE_COL, SURVEY_FK_COL, USER_FK_COL, SUBMITTED_DATE_COL,
                DELIVERED_DATE_COL, UUID_COL},
                Tables.SURVEY + "." + SURVEY_GROUP_ID_COL + "= ?",
                new String[]{String.valueOf(surveyGroupId)},
                null, null, sortBy);
        return cursor;
    }
    
    public Cursor getSurveyInstances(String surveyedLocaleId) {
        final String sortBy = 
            "case when " + DELIVERED_DATE_COL
                    + " is null then 0 else 1 end, " + DELIVERED_DATE_COL
                    + " desc";
        Cursor cursor = database.query(RESPONDENT_JOIN, new String[] {
                Tables.RESPONDENT + "." + PK_ID_COL, DISP_NAME_COL,
                SAVED_DATE_COL, SURVEY_FK_COL, USER_FK_COL, SUBMITTED_DATE_COL,
                DELIVERED_DATE_COL, UUID_COL, SURVEYED_LOCALE_ID_COL},
                Tables.RESPONDENT + "." + SURVEYED_LOCALE_ID_COL + "= ?",
                new String[]{String.valueOf(surveyedLocaleId)},
                null, null, sortBy);
        return cursor;
    }
    
    /**
     * Given a particular surveyedLocale and one of its surveys,
     * retrieves the ID of the last surveyInstance matching that criteria
     * @param surveyedLocaleId
     * @param surveyId
     * @return last surveyInstance with those attributes
     */
    public Long getLastSurveyInstance(String surveyedLocaleId, long surveyId) {
        Cursor cursor = database.query(Tables.RESPONDENT, 
                new String[] {PK_ID_COL, SURVEYED_LOCALE_ID_COL, SURVEY_FK_COL, SUBMITTED_DATE_COL},
                SURVEYED_LOCALE_ID_COL + "= ? AND " + SURVEY_FK_COL + "= ? AND " + SUBMITTED_DATE_COL + " IS NOT NULL",
                new String[]{surveyedLocaleId, String.valueOf(surveyId)},
                null, null,
                SUBMITTED_DATE_COL + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndexOrThrow(PK_ID_COL));
        }
        
        return null;
    }
    
    public String getSurveyedLocaleId(long surveyInstanceId) {
        Cursor cursor = database.query(RESPONDENT_JOIN, new String[] {
                Tables.RESPONDENT + "." + PK_ID_COL, SURVEYED_LOCALE_ID_COL},
                Tables.RESPONDENT + "." + PK_ID_COL + "= ?",
                new String[]{String.valueOf(surveyInstanceId)},
                null, null, null);
        
        String id = null;
        if (cursor.moveToFirst()) {
            id = cursor.getString(cursor.getColumnIndexOrThrow(SURVEYED_LOCALE_ID_COL));
        }
        cursor.close();
        return id;
    }
    
    /**
     * Flag to indicate the type of locale update from a given response
     */
    public enum SurveyedLocaleMeta {NAME, GEOLOCATION};
    
    public void updateSurveyedLocale(long surveyInstanceId, String response, SurveyedLocaleMeta type) {
        if (!TextUtils.isEmpty(response)) {
            String surveyedLocaleId = getSurveyedLocaleId(surveyInstanceId);
            ContentValues surveyedLocaleValues = new ContentValues();
            
            QuestionResponse metaResponse = new QuestionResponse();
            metaResponse.setRespondentId(surveyInstanceId);
            metaResponse.setValue(response);
            metaResponse.setIncludeFlag("true");
            
            switch (type) {
                case NAME:
                    surveyedLocaleValues.put(SurveyedLocaleAttrs.NAME, response);
                    metaResponse.setType("META_NAME");
                    metaResponse.setQuestionId(ConstantUtil.QUESTION_LOCALE_NAME);
                    break;
                case GEOLOCATION:
                    String[] parts = response.split("\\|");
                    if (parts.length >= 2) {
                        surveyedLocaleValues.put(SurveyedLocaleAttrs.LATITUDE, Double.parseDouble(parts[0]));
                        surveyedLocaleValues.put(SurveyedLocaleAttrs.LONGITUDE, Double.parseDouble(parts[1]));
                    }
                    metaResponse.setType("META_GEO");
                    metaResponse.setQuestionId(ConstantUtil.QUESTION_LOCALE_GEO);
                    break;
            }
            
            // Update the surveyed locale info
            database.update(Tables.SURVEYED_LOCALE, surveyedLocaleValues,
                    SurveyedLocaleAttrs.SURVEYED_LOCALE_ID + " = ?",
                    new String[] {surveyedLocaleId});
            
            // Store the META_NAME/META_GEO as a response
            createOrUpdateSurveyResponse(metaResponse);
        }
    }
    
    /**
    * Filters surveyd locales based on the parameters passed in.
    * @param projectId
    * @param latitude
    * @param longitude
    * @param filterString
    * @param nearbyRadius
    * @return
    */
    public Cursor getFilteredSurveyedLocales(long surveyGroupId, Double latitude, Double longitude,
                Double nearbyRadius, int orderBy) {
        String queryString = "SELECT sl.*, MAX(r." + SUBMITTED_DATE_COL + ") as "+ SUBMITTED_DATE_COL + " FROM " 
                + Tables.SURVEYED_LOCALE + " AS sl LEFT JOIN " + Tables.RESPONDENT + " AS r ON "
                + "sl." + SurveyedLocaleAttrs.SURVEYED_LOCALE_ID + "=" + "r." + SURVEYED_LOCALE_ID_COL;
        String whereClause = " WHERE sl." + SurveyedLocaleAttrs.SURVEY_GROUP_ID + " =?";
        String groupBy = " GROUP BY sl." + SurveyedLocaleAttrs.SURVEYED_LOCALE_ID;
        String orderByStr = " ORDER BY " + SUBMITTED_DATE_COL + " DESC";// By date
        
        // location part
        if (orderBy == ConstantUtil.ORDER_BY_DISTANCE && latitude != null && longitude != null){
            // this is to correct the distance for the shortening at higher latitudes
            Double fudge = Math.pow(Math.cos(Math.toRadians(latitude)),2);
            
            // this uses a simple planar approximation of distance. this should be good enough for our purpose.
            String orderByTempl = " ORDER BY ((%s - " + SurveyedLocaleAttrs.LATITUDE + ") * (%s - " + SurveyedLocaleAttrs.LATITUDE + ") + (%s - " + SurveyedLocaleAttrs.LONGITUDE + ") * (%s - " + SurveyedLocaleAttrs.LONGITUDE + ") * %s)";
            orderByStr = String.format(orderByTempl, latitude, latitude, longitude, longitude, fudge);
        } 
        
        String[] whereValues = new String[] {String.valueOf(surveyGroupId)};
        Cursor cursor = database.rawQuery(queryString + whereClause + groupBy + orderByStr, whereValues);
        
        return cursor;
    }
    
    public int getSurveyedLocalesCount(long surveyGroupId) {
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + Tables.SURVEYED_LOCALE
                + " WHERE " + SurveyedLocaleAttrs.SURVEY_GROUP_ID + " = ?",
                new String[]{String.valueOf(surveyGroupId)});
        
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        
        return 0;
    }
    
    // ======================================================= //
    // =========== SurveyedLocales synchronization =========== //
    // ======================================================= //
    
    public void syncResponses(List<QuestionResponse> responses, long surveyInstanceId) {
        for (QuestionResponse response : responses) {
            Cursor cursor = database.query(Tables.RESPONSE, new String[] {
                    "survey_respondent_id, question_id"},
                    "survey_respondent_id = ? AND question_id = ?",
                    new String[] { String.valueOf(surveyInstanceId), response.getQuestionId()},
                    null, null, null);
                
            boolean exists = cursor.getCount() > 0;
            cursor.close();
                
            ContentValues values = new ContentValues();
            values.put(ANSWER_COL, response.getValue());
            values.put(ANSWER_TYPE_COL, response.getType());
            values.put(QUESTION_FK_COL, response.getQuestionId());
            values.put(INCLUDE_FLAG_COL, response.getIncludeFlag());
            values.put(SURVEY_RESPONDENT_ID_COL, surveyInstanceId);
                
            if (exists) {
                database.update(Tables.RESPONSE, values, 
                        "survey_respondent_id = ? AND question_id = ?",
                        new String[] { String.valueOf(surveyInstanceId), response.getQuestionId()});
            } else {
                database.insert(Tables.RESPONSE, null, values);
            }
        }
    }
    
    public void syncSurveyInstances(List<SurveyInstance> surveyInstances, String surveyedLocaleId) {
        for (SurveyInstance surveyInstance : surveyInstances) {
            Cursor cursor = database.query(Tables.RESPONDENT, new String[] {
                    PK_ID_COL, UUID_COL},
                    UUID_COL + " = ?",
                    new String[] { surveyInstance.getUuid()},
                    null, null, null);
                
            long id = -1;
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0);
            }
            cursor.close();
                
            ContentValues values = new ContentValues();
            values.put(SURVEY_FK_COL, surveyInstance.getSurveyId());
            values.put(SUBMITTED_DATE_COL, surveyInstance.getDate());
            values.put(SURVEYED_LOCALE_ID_COL, surveyedLocaleId);
            values.put(STATUS_COL, ConstantUtil.SUBMITTED_STATUS);// ???
                
            if (id != -1) {
                database.update(Tables.RESPONDENT, values, UUID_COL + " = ?", new String[] { surveyInstance.getUuid()});
            } else {
                values.put(UUID_COL, surveyInstance.getUuid());
                id = database.insert(Tables.RESPONDENT, null, values);
            }
            
            createTransmissionHistory(id, null, ConstantUtil.DOWNLOADED_STATUS);
                
            // Now the responses...
            syncResponses(surveyInstance.getResponses(), id);
        }
    }
    
    public void syncSurveyedLocales(List<SurveyedLocale> surveyedLocales) {
        for (SurveyedLocale surveyedLocale : surveyedLocales) {
            try {
                database.beginTransaction();
                
                ContentValues values = new ContentValues();
                values.put(SurveyedLocaleAttrs.SURVEYED_LOCALE_ID, surveyedLocale.getId());
                values.put(SurveyedLocaleAttrs.SURVEY_GROUP_ID, surveyedLocale.getSurveyGroupId());
                values.put(SurveyedLocaleAttrs.NAME, surveyedLocale.getName());
                values.put(SurveyedLocaleAttrs.LATITUDE, surveyedLocale.getLatitude());
                values.put(SurveyedLocaleAttrs.LONGITUDE, surveyedLocale.getLongitude());
                database.insert(Tables.SURVEYED_LOCALE, null, values);
                
                syncSurveyInstances(surveyedLocale.getSurveyInstances(), surveyedLocale.getId());
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
    }
    
    /**
     * Get the synchronization time for a particular survey group.
     * @param surveyGroupId id of the SurveyGroup
     * @return time if exists for this key, null otherwise
     */
    public String getSyncTime(long surveyGroupId) {
        Cursor cursor = database.query(Tables.SYNC_TIME, 
                new String[] {SyncTimeAttrs.SURVEY_GROUP_ID, SyncTimeAttrs.TIME},
                SyncTimeAttrs.SURVEY_GROUP_ID + "=?",
                new String[] {String.valueOf(surveyGroupId)},
                null, null, null);
        
        String time = null;
        if (cursor.moveToFirst()) {
            time = cursor.getString(cursor.getColumnIndexOrThrow(SyncTimeAttrs.TIME));
        }
        cursor.close();
        return time;
    }
    
    /**
     * Save the time of synchronization time for a particular SurveyGroup
     * @param surveyGroupId id of the SurveyGroup
     * @param time String containing the timestamp
     */
    public void setSyncTime(long surveyGroupId, String time) {
        ContentValues values = new ContentValues();
        values.put(SyncTimeAttrs.SURVEY_GROUP_ID, surveyGroupId);
        values.put(SyncTimeAttrs.TIME, time);
        database.insert(Tables.SYNC_TIME, null, values);
    }

}
