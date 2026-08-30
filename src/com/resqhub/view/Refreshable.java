package com.resqhub.view;

/** Implemented by module panels whose data must reload every time the
 *  dashboard brings their card back on screen. */
public interface Refreshable {

    void refreshData();
}
