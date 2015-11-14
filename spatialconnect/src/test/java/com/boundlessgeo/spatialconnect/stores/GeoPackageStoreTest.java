package com.boundlessgeo.spatialconnect.stores;


import android.content.Context;

import com.boundlessgeo.spatialconnect.config.SCStoreConfig;
import com.boundlessgeo.spatialconnect.dataAdapter.GeoPackageAdapter;
import com.boundlessgeo.spatialconnect.geometries.SCSpatialFeature;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * https://developer.android.com/training/testing/unit-testing/local-unit-tests.html
 **/
@RunWith(MockitoJUnitRunner.class)
public class GeoPackageStoreTest {

    @Mock
    Context mockContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void testCreateNewFeatureRow() {
        // setup the environment
        SCStoreConfig mockConfig = Mockito.mock(SCStoreConfig.class);
        when(mockConfig.getName()).thenReturn("mockGeoPackageName");

        GeoPackage mockGeoPackage = Mockito.mock(GeoPackage.class);
        when(mockGeoPackage.getFeatureDao(anyString())).thenReturn(Mockito.mock(FeatureDao.class));

        GeoPackageManager mockManager = Mockito.mock(GeoPackageManager.class);
        when(mockManager.open(anyString())).thenReturn(mockGeoPackage);

        GeoPackageAdapter mockAdapter = Mockito.mock(GeoPackageAdapter.class);
        when(mockAdapter.getGeoPackageManager()).thenReturn(mockManager);

        GeoPackageStore store = Mockito.mock(GeoPackageStore.class);

        SCSpatialFeature scFeature = new SCSpatialFeature();
        scFeature.setId("123.456.789");


        // execute the function
        FeatureRow featureRow = store.toFeatureRow(scFeature);

        // verify
        verify(store).toFeatureRow(scFeature);

        // assertEquals(789, featureRow.getId());

    }

    @Test
    public void testGetStoreId() {
        GeoPackageStore mockStore = Mockito.mock(GeoPackageStore.class);
        String storeId = mockStore.getStoreId("123.456.789");
        assertEquals("The row id should be 123", "123", storeId);
    }

    @Test
    public void testGetFeatureTableName() {
        GeoPackageStore mockStore = Mockito.mock(GeoPackageStore.class);
        String storeId = mockStore.getFeatureTableName("123.456.789");
        assertEquals("The row id should be 456", "456", storeId);
    }

    @Test
    public void testGetRowId() {
        GeoPackageStore mockStore = Mockito.mock(GeoPackageStore.class);
        String rowId = mockStore.getRowId("123.456.789");
        assertEquals("The row id should be 789", "789", rowId);
    }

}
