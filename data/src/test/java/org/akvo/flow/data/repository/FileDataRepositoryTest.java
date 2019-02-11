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

package org.akvo.flow.data.repository;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.files.BitmapHelper;
import org.akvo.flow.data.datasource.files.ImageDataSource;
import org.akvo.flow.data.datasource.files.MediaResolverHelper;
import org.akvo.flow.data.datasource.preferences.SharedPreferencesDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.observers.TestObserver;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BitmapFactory.class, TextUtils.class})
public class FileDataRepositoryTest {

    @Mock
    SharedPreferencesDataSource mockSharedPrefs;

    @Mock
    Uri mockUri;

    @Mock
    InputStream mockFileInputStream;

    @Mock
    ParcelFileDescriptor mockParcelFileDescriptor;

    @Mock
    FileDescriptor mockFileDescriptor;

    @Mock
    Bitmap mockBitmap;

    @Mock
    MediaResolverHelper mockMediaResolverHelper;

    @Mock
    BitmapHelper mockBitmapHelper;

    private FileDataRepository fileDataRepository;

    @Before
    public void setUp() throws IOException {
        PowerMockito.mockStatic(BitmapFactory.class);
        PowerMockito.when(BitmapFactory
                .decodeFileDescriptor(any(FileDescriptor.class), any(Rect.class),
                        any(BitmapFactory.Options.class))).thenReturn(mockBitmap);

        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                return !(a != null && a.length() > 0);
            }
        });

        ImageDataSource imageDataSource = new ImageDataSource(mockMediaResolverHelper, mockBitmapHelper);
        DataSourceFactory dataSourceFactory = new DataSourceFactory(mockSharedPrefs,
                imageDataSource, null, null, null, null);
        fileDataRepository = new FileDataRepository(dataSourceFactory);
        doNothing().when(mockParcelFileDescriptor).close();

        when(mockMediaResolverHelper.removeDuplicateImage(mockUri)).thenReturn(true);
    }

    @Test
    public void shouldReturnErrorIfWrongUri() {
        when(mockMediaResolverHelper.getInputStreamFromUri(any(Uri.class))).thenReturn(null);

        TestObserver observer = new TestObserver<Boolean>();

        fileDataRepository.copyResizedImage(mockUri, "123", 0, true).subscribe(observer);

        observer.assertNoValues();
        assertEquals(1, observer.errorCount());
        assertTrue(observer.errors().get(0) instanceof Exception);
    }

    @Test
    public void shouldReturnErrorIfNullFileDescriptor() {
        when(mockMediaResolverHelper.openFileDescriptor((any(Uri.class))))
                .thenReturn(null);

        TestObserver observer = new TestObserver<Boolean>();

        fileDataRepository.copyResizedImage(mockUri, "123", 0, true).subscribe(observer);

        observer.assertNoValues();
        assertEquals(1, observer.errorCount());
        assertTrue(observer.errors().get(0) instanceof Exception);
    }

    @Test
    public void shouldReturnErrorIfNullBitmap() {
        when(mockMediaResolverHelper.openFileDescriptor((any(Uri.class))))
                .thenReturn(mockParcelFileDescriptor);
        when(mockParcelFileDescriptor.getFileDescriptor()).thenReturn(mockFileDescriptor);
        when(mockBitmapHelper.getBitmap(anyInt(), any(ParcelFileDescriptor.class))).thenReturn(null);

        TestObserver observer = new TestObserver<Boolean>();

        fileDataRepository.copyResizedImage(mockUri, "123", 0, true).subscribe(observer);

        observer.assertNoValues();
        assertEquals(1, observer.errorCount());
        assertTrue(observer.errors().get(0) instanceof Exception);
    }

    @Test
    public void shouldReturnNoErrorsIfAllGoesWell() {
        when(mockMediaResolverHelper.openFileDescriptor((any(Uri.class))))
                .thenReturn(mockParcelFileDescriptor);
        when(mockParcelFileDescriptor.getFileDescriptor()).thenReturn(mockFileDescriptor);
        when(mockBitmapHelper.getBitmap(anyInt(), any(ParcelFileDescriptor.class))).thenReturn(mockBitmap);
        when(mockParcelFileDescriptor.getFileDescriptor()).thenReturn(mockFileDescriptor);
     /*   when(mockBitmap
                .compress(any(Bitmap.CompressFormat.class), anyInt(), any(OutputStream.class)))
                .thenReturn(true);*/

        TestObserver observer = new TestObserver<Boolean>();

        fileDataRepository.copyResizedImage(mockUri, "123", 0, true).subscribe(observer);

        observer.assertNoErrors();
        assertTrue((Boolean) observer.values().get(0));
    }
}
