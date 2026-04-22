package com.derwin.prepforge.common.utils;

import java.time.Instant;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtils {

    public Instant nowUtc() {
        return Instant.now();
    }
}
