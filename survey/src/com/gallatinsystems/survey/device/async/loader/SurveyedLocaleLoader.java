/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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

package com.gallatinsystems.survey.device.async.loader;

import android.content.Context;
import android.database.Cursor;

import com.gallatinsystems.survey.device.async.loader.base.DataLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.util.ConstantUtil;

public class SurveyedLocaleLoader extends DataLoader<Cursor> {
    private long mSurveyGroupId;
    private double mLatitude;
    private double mLongitude;
    private double mRadius;
    
    private int mOrderBy;

    public SurveyedLocaleLoader(Context context, SurveyDbAdapter db, long surveyGroupId,
            double latitude, double longitude, double radius, int orderBy) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mLatitude = latitude;
        mLongitude = longitude;
        mRadius = radius;
        mOrderBy = orderBy;
    }
    
    public SurveyedLocaleLoader(Context context, SurveyDbAdapter db, long surveyGroupId, int orderBy) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
        mOrderBy = orderBy;
    }

    @Override
    protected Cursor loadData(SurveyDbAdapter database) {
        switch (mOrderBy) {
            case ConstantUtil.ORDER_BY_DISTANCE:
            case ConstantUtil.ORDER_BY_DATE:
                return database.getFilteredSurveyedLocales(mSurveyGroupId, mLatitude, mLongitude, mRadius, mOrderBy);
            case ConstantUtil.ORDER_BY_NONE:
                return database.getSurveyedLocales(mSurveyGroupId);
            default:
                return null;
        }
    }

}
