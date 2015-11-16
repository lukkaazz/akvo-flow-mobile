/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain;

import org.akvo.flow.domain.response.value.CascadeValue;
import org.akvo.flow.ui.view.CascadeQuestionView;
import org.akvo.flow.util.ConstantUtil;

public class QuestionResponse {
    private String value;
    private String type;
    private Long id;
    private Long respondentId;
    private String questionId;
    private boolean includeFlag;
    private String scoredValue;
    private String strength;

    public QuestionResponse(Long id, Long respondentId, String qId, String val,
            String t, boolean includeFlag) {
        this(id, respondentId, qId, val, t, includeFlag, null);
    }

    public QuestionResponse(String val, String t, String questionId) {
        this(null, null, questionId, val, t, true);
    }

    public QuestionResponse(Long id, Long respondentId, String qId, String val,
            String t, boolean includeFlag, String strength) {
        this.id = id;
        value = val;
        type = t;
        this.respondentId = respondentId;
        questionId = qId;
        this.includeFlag = includeFlag;
        this.strength = strength;
    }

    public QuestionResponse() {
        id = null;
        type = null;
        value = null;
        respondentId = null;
        questionId = null;
        includeFlag = true;
        strength = null;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public boolean getIncludeFlag() {
        return includeFlag;
    }

    public void setIncludeFlag(boolean includeFlag) {
        this.includeFlag = includeFlag;
    }

    public boolean isValid() {
        if (!ConstantUtil.OTHER_RESPONSE_TYPE.equals(type)) {
            // if the response isn't "OTHER" then we have to check that it has a
            // value that isn't just a blank
            if (value == null || value.trim().length() == 0) {
                return false;
            }
            // now check that, if it's a geo question, we have something specified
            if (ConstantUtil.GEO_RESPONSE_TYPE.equals(type)) {
                String[] tokens = value.split("\\|", -1);
                if (tokens.length >= 2) {
                    // at least the first 2 tokens must be numeric
                    for (int i = 0; i < 2; i++) {
                        String token = tokens[i];
                        try {
                            if (token.trim().length() > 0) {
                                Double.parseDouble(token);
                            } else {
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public void setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScoredValue() {
        return scoredValue;
    }

    public void setScoredValue(String scoredValue) {
        this.scoredValue = scoredValue;
    }

    public boolean hasValue() {
        boolean hasVal = false;
        if (value != null && value.trim().length() > 0) {
            hasVal = true;
        }
        return hasVal;
    }

    /**
     * Build a human-readable representation of the response.
     * Based on the response type, this means handling and parsing each value in a different way.
     */
    public String getDatapointNameValue() {
        if (type == null || value == null) {
            return "";
        }

        String name;
        switch (type) {
            case ConstantUtil.CASCADE_RESPONSE_TYPE:
                name = getCascadeDatapointName();
                break;
            default:
                name = value;
                break;
        }

        name = name.replaceAll("\\s+", " ");// Trim line breaks, multiple spaces, etc
        name = name.replaceAll("\\s*\\|\\s*", " - ");// Replace pipes with hyphens

        return name.trim();
    }

    private String getCascadeDatapointName() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (CascadeValue cv : CascadeQuestionView.loadValues(value)) {
            if (!first) {
                builder.append(" - ");
            }
            builder.append(cv.getName());
            first = false;
        }

        return builder.toString();
    }

}
