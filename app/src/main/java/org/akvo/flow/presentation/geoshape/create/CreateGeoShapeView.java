/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.presentation.geoshape.create;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.akvo.flow.presentation.geoshape.entities.Shape;
import org.akvo.flow.presentation.geoshape.entities.ViewFeatures;

import java.util.List;

public interface CreateGeoShapeView {

    void displayDeleteShapeDialog();

    void displayDeletePointDialog();

    void displaySelectedShapeInfo(Shape shape);

    void displayMapItems(ViewFeatures viewFeatures);

    void enablePointDrawMode();

    void enableLineDrawMode();

    void enableAreaDrawMode();

    void updateSources(FeatureCollection features, FeatureCollection pointList);

    void updateMenu();

    void displayNewMapStyle(FeatureCollection shapeFeatures, FeatureCollection pointFeatures,
            List<LatLng> listOfCoordinates);

    void setShapeResult(String toJson);

    void setCanceledResult();
}
