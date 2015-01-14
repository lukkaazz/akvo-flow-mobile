package org.akvo.flow.ui.map;

import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.akvo.flow.R;

public class PolygonFeature extends Feature {
    public static final String GEOMETRY_TYPE = "Polygon";

    private static final int FILL_COLOR = 0x88736357;

    private Polygon mPolygon;

    public PolygonFeature(GoogleMap map) {
        super(map);
    }

    @Override
    public void addPoint(LatLng point) {
        super.addPoint(point);
        if (mPolygon == null) {
            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.strokeColor(mSelected ? STROKE_COLOR_SELECTED : STROKE_COLOR_DEFAULT);
            polygonOptions.fillColor(FILL_COLOR);
            polygonOptions.add(point);// Polygon cannot be created without points
            mPolygon = mMap.addPolygon(polygonOptions);
        } else {
            mPolygon.setPoints(mPoints);
        }
    }

    @Override
    public void removePoint() {
        super.removePoint();
        if (mPoints.isEmpty()) {
            mPolygon.remove();
            mPolygon = null;
        } else {
            mPolygon.setPoints(mPoints);
        }
    }

    @Override
    public void delete() {
        super.delete();
        if (mPolygon != null) {
            mPolygon.remove();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (mPolygon != null) {
            mPolygon.setStrokeColor(mSelected ? STROKE_COLOR_SELECTED : STROKE_COLOR_DEFAULT);
            if (!mPoints.isEmpty()) {
                mPolygon.setPoints(mPoints);
            }
        }

        // Properties
        float length = 0f;
        // Init previous with last point, so we compute the last-first distance as well
        LatLng previous = mPoints.size() > 2 ? mPoints.get(mPoints.size()-1) : null;
        for (LatLng point : mPoints) {
            if (previous != null) {
                float[] distance = new float[1];
                Location.distanceBetween(previous.latitude, previous.longitude, point.latitude, point.longitude, distance);
                length += distance[0];
            }
            previous = point;
        }
        String lengthVal = String.format("%.2f", length);
        mProperties.add(new Property("length", lengthVal, "Length", lengthVal + "m"));
    }

    @Override
    public int getTitle() {
        return R.string.geoshape_area;
    }

    @Override
    public String geoGeometryType() {
        return GEOMETRY_TYPE;
    }

    @Override
    public boolean highlightPrevious(int position) {
        return true;
    }

}
