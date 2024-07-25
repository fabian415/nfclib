package com.advantech.nfclib.image;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.ByteArrayOutputStream;

public class ImageConverter {
    private static final int MIN = 0;
    private static final int MAX = 255;
    private static final int CENTER = 128;

    public byte[] rgb2_forEPD37(char[][] rgbTable) {
        // 416*240
        byte[] os = new byte[rgbTable.length * rgbTable[0].length / 4];
        int index = 0;

        for (int i = 0; i < rgbTable.length; i++) {
            for (int j = 0; j < rgbTable[i].length; j += 8) {
                byte total = 0;
                for (int k = 0; k < 8; k++) {
                    char pixel = rgbTable[rgbTable.length - i - 1][j + k];
                    if (pixel == 'w')
                    {
                        int res = 0b00000001 << (7-k);
                        total = (byte)(total + res);
                    }
                    else if (pixel == 'r')
                    {
                        int res = 0b00000001 << (7-k);
                        total = (byte)(total + res);
                    }
                    else if (pixel == 'b')
                    {
                        int res = 0;
                        total = (byte)(total + res);
                    }
                }
                os[index] = total;
                index++;
            }
        }

        for (int i = 0; i < rgbTable.length; i++) {
            for (int j = 0; j < rgbTable[0].length; j += 8) {
                byte total = 0;
                for (int k = 0; k < 8; k++) {
                    char pixel = rgbTable[rgbTable.length - i - 1][j + k];
                    if (pixel == 'r') {
                        int res = 0b00000001 << (7 - k);
                        total = (byte)(total + res);
                    } else {
                        int res = 0;
                        total = (byte)(total + res);
                    }
                }
                os[index] = total;
                index++;
            }
        }
        return os;
    }

    public char[][] img2rgb_forEPD37(Bitmap image) {
        int MAX = 255; // 最大颜色值
        int MIN = 0;   // 最小颜色值

        char[][] table = new char[image.getWidth()][image.getHeight()];

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {

                int color = image.getPixel(x, image.getHeight() - y - 1);
                int b = color & 0xff;
                int g = (color & 0xff00) >> 8;
                int r = (color & 0xff0000) >> 16;

                if ((r == MAX) && (g == MAX) && (b == MAX)) {
                    table[x][y] = 'w';
                } else if ((r == MIN) && (g == MIN) && (b == MIN)) {
                    table[x][y] = 'b';
                } else if ((r == MAX) && (g == MIN) && (b == MIN)) {
                    table[x][y] = 'r';
                } else {
                    if (r > 200 && (50 < (r - g)) && (50 < (r - b)))
                        table[x][y] = 'r';
                    else if (r > 150 && 10 > g && 10 > b)
                        table[x][y] = 'r';
                    else {
                        double level = r * 0.299 + g * 0.587 + b * 0.144;
                        if (level > 192)
                            table[x][y] = 'w';
                        else
                            table[x][y] = 'b';
                    }
                }
            }
        }
        //image.recycle();
        return table;
    }



    public byte[] img_forEPD29(Bitmap bitmap) {
        int width, height, index;
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        index = 0;
        int count = width * height / 8;
        byte[] os = new byte[count];

        for(int j=0;j<width;j++) {
            for(int i=0;i<height;i+=8) {
                int v = 0;
                for(int k=0;k<8;k++) {
                    int color = bitmap.getPixel(width-j-1, i+k);
                    // convert to BW
                    v *= 2;
                    if(ConvertPixel(color)) {
                        v |= 1;
                    }
                }
                os[index] = (byte) (v&0xFF);
                index++;
            }
        }
        return os;
    }

    public byte[] img_forEPD302(Bitmap bitmap) {
        int width, height, index;
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        index = 0;
        int count = width * height / 8;
        byte[] os = new byte[count];

        for(int j=0;j<width;j++) {
            for(int i=0;i<height;i+=8) {
                int v = 0;
                for(int k=0;k<8;k++) {
                    int color = bitmap.getPixel(width-j-1, i+k);
                    // convert to BW
                    v *= 2;
                    if(ConvertPixel(color)) {
                        v |= 1;
                    }
                }
                os[index] = (byte) (v&0xFF);
                index++;
            }
        }
        return os;
    }

    public byte[] img_forEPD304(Bitmap bitmap) {
        int width, height, index;
        width = bitmap.getWidth();
        height = bitmap.getHeight();

        char[][] table = new char[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = bitmap.getPixel(x,height - y - 1);
                int b = color & 0xff;
                int g = (color & 0xff00) >> 8;
                int r = (color & 0xff0000) >> 16;

                if ((r == MAX) && (g == MAX) && ((b == MAX))) {
                    table[x][y] = 'w';
                }
                else if ((r == MIN) && (g == MIN) && ((b == MIN))) {
                    table[x][y] = 'B';
                }
                else if ((r == MAX) && (g == MIN) && ((b == MIN))) {
                    table[x][y] = 'r';
                }
                else if ((r == MAX) && (g == MAX) && ((b == MIN))) {
                    table[x][y] = 'y';
                }
                else {
//                    RGBTriple rgbTriple = ImageUtil.findNearestColor(color, ImageUtil.bwry);
//                    if (rgbTriple.channels[0] == MAX && rgbTriple.channels[1] == MAX && rgbTriple.channels[2] == MAX) {
//                        table[x][y] = 'w';
//                    } else if (rgbTriple.channels[0] == MIN && rgbTriple.channels[1] == MIN && rgbTriple.channels[2] == MIN) {
//                        table[x][y] = 'B';
//                    } else if (rgbTriple.channels[0] == MAX && rgbTriple.channels[1] == MIN && rgbTriple.channels[2] == MIN) {
//                        table[x][y] = 'r';
//                    } else if (rgbTriple.channels[0] == MAX && rgbTriple.channels[1] == MAX && rgbTriple.channels[2] == MIN) {
//                        table[x][y] = 'y';
//                    }
                    if (r > 127 && (100 < (r - g)) && (100 < (r - b)))
                        table[x][y] = 'r';
                    else if (r > 180 && g > 158 && b < 169) {
                        table[x][y] = 'y';
                    } else { // gray color
                        double level = r * 0.299 + g * 0.587 + b * 0.144; // covert to Y(YUV)
                        if (level > 184)
                            table[x][y] = 'w';
                        else
                            table[x][y] = 'b';
                    }
                }
            }
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j <= (table[0].length - 1); j = j + 4) { // height
                int total = 0;
                for (int k = 0; k < 4; k++) {
                    int res = 0;
//                    char pixel = table[i][j+k];
                    char pixel = table[table.length - i - 1][j + k];
                    if (pixel == 'w') {
                        res = 0b1;
                    } else if (pixel == 'r') {
                        res = 0b11;
                    } else if (pixel == 'y') {
                        res = 0b10;
                    }
                    total = total + res;
                    if (k < (4-1)) {
                        total = total << (8/4);
                    }
                }
                os.write((byte)total);
            }
        }
        return os.toByteArray();
    }

    public  boolean ConvertPixel(int color) {
        int r = ((color>>16)&0xFF);
        int g = ((color>>8)&0xFF);
        int b = ((color)&0xFF);

        int dis_w = (r-255)*(r-255)+(g-255)*(g-255)+(b-255)*(b-255);
        int dis_b = r*r+g*g+b*b;

        if(dis_b < dis_w)
            return false;
        return true;
    }

    public Bitmap resizeImage(Bitmap image, int width, int height) {
        Bitmap destImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(destImage);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        Rect srcRect = new Rect(0, 0, image.getWidth(), image.getHeight());
        RectF destRect = new RectF(0, 0, width, height);

        canvas.drawBitmap(image, srcRect, destRect, paint);

        return destImage;
    }
}

