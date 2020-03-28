/*
 * Copyright (C) 2020 Peter Österlund
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.margaritov.preference.colorpicker;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AHSVColorTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void alpha() {
        AHSVColor color = new AHSVColor();
        for (int i = 0; i < 255; i++) {
            color.setAlpha(i);
            assertEquals(i, color.getAlpha());
        }
    }

    @Test
    public void hsv() {
        AHSVColor color = new AHSVColor();
        double[] hsv = new double[3];
        for (int i = 0; i < 360; i++) {
            hsv[0] = i;
            hsv[1] = (i % 17) / 17;
            hsv[2] = (i % 11) / 11;
            color.setHSV(hsv);
            double[] ret = color.getHSV();
            assertEquals(hsv[0], ret[0], 1e-10);
            assertEquals(hsv[1], ret[1], 1e-10);
            assertEquals(hsv[2], ret[2], 1e-10);
        }
    }

    @Test
    public void rgb() {
        AHSVColor color = new AHSVColor();
        for (int i = 0; i < 255; i++) {
            int r = (i * 3413) % 255;
            int g = (i * 113) % 255;
            int b = (i * 1847) % 255;
            int c = 0xff000000 + (r << 16) + (g << 8) + b;
            color.setARGB(c);
            int c2 = color.getARGB();
            int r2 = (c2 & 0x00ff0000) >>> 16;
            int g2 = (c2 & 0x0000ff00) >>> 8;
            int b2 = (c2 & 0x000000ff);
            assertEquals(r, r2);
            assertEquals(g, g2);
            assertEquals(b, b2);
        }
    }
}
