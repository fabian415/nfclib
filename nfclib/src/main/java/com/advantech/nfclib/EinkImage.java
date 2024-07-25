package com.advantech.nfclib;


import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.advantech.nfclib.image.ImageConverter;

import net.jpountz.lz4.LZ4Factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Created by Fabian.Chung on 2024/07/17.
 */

public class EinkImage {

    private static String TAG = "EinkImage";

    final static byte [] font5x5 = new byte[] {
            0x7c,0x4c,0x54,0x64,0x7c, // 0
            0x10,0x30,0x10,0x10,0x38,
            0x78,0x04,0x38,0x40,0x7c,
            0x7c,0x04,0x38,0x04,0x7c,
            0x40,0x40,0x50,0x7c,0x10,
            0x7c,0x40,0x78,0x04,0x78,
            0x7c,0x40,0x7c,0x44,0x7c,
            0x7c,0x04,0x08,0x10,0x10,
            0x7c,0x44,0x7c,0x44,0x7c,
            0x7c,0x44,0x7c,0x04,0x7c, // 9
            0x7c,0x44,0x44,0x7c,0x44,
            0x7c,0x44,0x78,0x44,0x7c,
            0x7c,0x40,0x40,0x40,0x7c,
            0x78,0x44,0x44,0x44,0x78,
            0x7c,0x40,0x78,0x40,0x7c,
            0x7c,0x40,0x70,0x40,0x40,
    };

    final static byte [] font8x8 = new byte[] {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x18, 0x3C, 0x3C, 0x18, 0x18, 0x00, 0x18, 0x00,
            0x36, 0x36, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x36, 0x36, 0x7F, 0x36, 0x7F, 0x36, 0x36, 0x00,
            0x0C, 0x3E, 0x03, 0x1E, 0x30, 0x1F, 0x0C, 0x00,
            0x00, 0x63, 0x33, 0x18, 0x0C, 0x66, 0x63, 0x00,
            0x1C, 0x36, 0x1C, 0x6E, 0x3B, 0x33, 0x6E, 0x00,
            0x06, 0x06, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x18, 0x0C, 0x06, 0x06, 0x06, 0x0C, 0x18, 0x00,
            0x06, 0x0C, 0x18, 0x18, 0x18, 0x0C, 0x06, 0x00,
            0x00, 0x66, 0x3C, (byte) 0xFF, 0x3C, 0x66, 0x00, 0x00,
            0x00, 0x0C, 0x0C, 0x3F, 0x0C, 0x0C, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x0C, 0x06,
            0x00, 0x00, 0x00, 0x3F, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x0C, 0x00,
            0x60, 0x30, 0x18, 0x0C, 0x06, 0x03, 0x01, 0x00,
            0x3E, 0x63, 0x73, 0x7B, 0x6F, 0x67, 0x3E, 0x00,
            0x0C, 0x0E, 0x0C, 0x0C, 0x0C, 0x0C, 0x3F, 0x00,
            0x1E, 0x33, 0x30, 0x1C, 0x06, 0x33, 0x3F, 0x00,
            0x1E, 0x33, 0x30, 0x1C, 0x30, 0x33, 0x1E, 0x00,
            0x38, 0x3C, 0x36, 0x33, 0x7F, 0x30, 0x78, 0x00,
            0x3F, 0x03, 0x1F, 0x30, 0x30, 0x33, 0x1E, 0x00,
            0x1C, 0x06, 0x03, 0x1F, 0x33, 0x33, 0x1E, 0x00,
            0x3F, 0x33, 0x30, 0x18, 0x0C, 0x0C, 0x0C, 0x00,
            0x1E, 0x33, 0x33, 0x1E, 0x33, 0x33, 0x1E, 0x00,
            0x1E, 0x33, 0x33, 0x3E, 0x30, 0x18, 0x0E, 0x00,
            0x00, 0x0C, 0x0C, 0x00, 0x00, 0x0C, 0x0C, 0x00,
            0x00, 0x0C, 0x0C, 0x00, 0x00, 0x0C, 0x0C, 0x06,
            0x18, 0x0C, 0x06, 0x03, 0x06, 0x0C, 0x18, 0x00,
            0x00, 0x00, 0x3F, 0x00, 0x00, 0x3F, 0x00, 0x00,
            0x06, 0x0C, 0x18, 0x30, 0x18, 0x0C, 0x06, 0x00,
            0x1E, 0x33, 0x30, 0x18, 0x0C, 0x00, 0x0C, 0x00,
            0x3E, 0x63, 0x7B, 0x7B, 0x7B, 0x03, 0x1E, 0x00,
            0x0C, 0x1E, 0x33, 0x33, 0x3F, 0x33, 0x33, 0x00,
            0x3F, 0x66, 0x66, 0x3E, 0x66, 0x66, 0x3F, 0x00,
            0x3C, 0x66, 0x03, 0x03, 0x03, 0x66, 0x3C, 0x00,
            0x1F, 0x36, 0x66, 0x66, 0x66, 0x36, 0x1F, 0x00,
            0x7F, 0x46, 0x16, 0x1E, 0x16, 0x46, 0x7F, 0x00,
            0x7F, 0x46, 0x16, 0x1E, 0x16, 0x06, 0x0F, 0x00,
            0x3C, 0x66, 0x03, 0x03, 0x73, 0x66, 0x7C, 0x00,
            0x33, 0x33, 0x33, 0x3F, 0x33, 0x33, 0x33, 0x00,
            0x1E, 0x0C, 0x0C, 0x0C, 0x0C, 0x0C, 0x1E, 0x00,
            0x78, 0x30, 0x30, 0x30, 0x33, 0x33, 0x1E, 0x00,
            0x67, 0x66, 0x36, 0x1E, 0x36, 0x66, 0x67, 0x00,
            0x0F, 0x06, 0x06, 0x06, 0x46, 0x66, 0x7F, 0x00,
            0x63, 0x77, 0x7F, 0x7F, 0x6B, 0x63, 0x63, 0x00,
            0x63, 0x67, 0x6F, 0x7B, 0x73, 0x63, 0x63, 0x00,
            0x1C, 0x36, 0x63, 0x63, 0x63, 0x36, 0x1C, 0x00,
            0x3F, 0x66, 0x66, 0x3E, 0x06, 0x06, 0x0F, 0x00,
            0x1E, 0x33, 0x33, 0x33, 0x3B, 0x1E, 0x38, 0x00,
            0x3F, 0x66, 0x66, 0x3E, 0x36, 0x66, 0x67, 0x00,
            0x1E, 0x33, 0x07, 0x0E, 0x38, 0x33, 0x1E, 0x00,
            0x3F, 0x2D, 0x0C, 0x0C, 0x0C, 0x0C, 0x1E, 0x00,
            0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x3F, 0x00,
            0x33, 0x33, 0x33, 0x33, 0x33, 0x1E, 0x0C, 0x00,
            0x63, 0x63, 0x63, 0x6B, 0x7F, 0x77, 0x63, 0x00,
            0x63, 0x63, 0x36, 0x1C, 0x1C, 0x36, 0x63, 0x00,
            0x33, 0x33, 0x33, 0x1E, 0x0C, 0x0C, 0x1E, 0x00,
            0x7F, 0x63, 0x31, 0x18, 0x4C, 0x66, 0x7F, 0x00,
            0x1E, 0x06, 0x06, 0x06, 0x06, 0x06, 0x1E, 0x00,
            0x03, 0x06, 0x0C, 0x18, 0x30, 0x60, 0x40, 0x00,
            0x1E, 0x18, 0x18, 0x18, 0x18, 0x18, 0x1E, 0x00,
            0x08, 0x1C, 0x36, 0x63, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFF,
            0x0C, 0x0C, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x1E, 0x30, 0x3E, 0x33, 0x6E, 0x00,
            0x07, 0x06, 0x06, 0x3E, 0x66, 0x66, 0x3B, 0x00,
            0x00, 0x00, 0x1E, 0x33, 0x03, 0x33, 0x1E, 0x00,
            0x38, 0x30, 0x30, 0x3e, 0x33, 0x33, 0x6E, 0x00,
            0x00, 0x00, 0x1E, 0x33, 0x3f, 0x03, 0x1E, 0x00,
            0x1C, 0x36, 0x06, 0x0f, 0x06, 0x06, 0x0F, 0x00,
            0x00, 0x00, 0x6E, 0x33, 0x33, 0x3E, 0x30, 0x1F,
            0x07, 0x06, 0x36, 0x6E, 0x66, 0x66, 0x67, 0x00,
            0x0C, 0x00, 0x0E, 0x0C, 0x0C, 0x0C, 0x1E, 0x00,
            0x30, 0x00, 0x30, 0x30, 0x30, 0x33, 0x33, 0x1E,
            0x07, 0x06, 0x66, 0x36, 0x1E, 0x36, 0x67, 0x00,
            0x0E, 0x0C, 0x0C, 0x0C, 0x0C, 0x0C, 0x1E, 0x00,
            0x00, 0x00, 0x33, 0x7F, 0x7F, 0x6B, 0x63, 0x00,
            0x00, 0x00, 0x1F, 0x33, 0x33, 0x33, 0x33, 0x00,
            0x00, 0x00, 0x1E, 0x33, 0x33, 0x33, 0x1E, 0x00,
            0x00, 0x00, 0x3B, 0x66, 0x66, 0x3E, 0x06, 0x0F,
            0x00, 0x00, 0x6E, 0x33, 0x33, 0x3E, 0x30, 0x78,
            0x00, 0x00, 0x3B, 0x6E, 0x66, 0x06, 0x0F, 0x00,
            0x00, 0x00, 0x3E, 0x03, 0x1E, 0x30, 0x1F, 0x00,
            0x08, 0x0C, 0x3E, 0x0C, 0x0C, 0x2C, 0x18, 0x00,
            0x00, 0x00, 0x33, 0x33, 0x33, 0x33, 0x6E, 0x00,
            0x00, 0x00, 0x33, 0x33, 0x33, 0x1E, 0x0C, 0x00,
            0x00, 0x00, 0x63, 0x6B, 0x7F, 0x7F, 0x36, 0x00,
            0x00, 0x00, 0x63, 0x36, 0x1C, 0x36, 0x63, 0x00,
            0x00, 0x00, 0x33, 0x33, 0x33, 0x3E, 0x30, 0x1F,
            0x00, 0x00, 0x3F, 0x19, 0x0C, 0x26, 0x3F, 0x00,
            0x38, 0x0C, 0x0C, 0x07, 0x0C, 0x0C, 0x38, 0x00,
            0x18, 0x18, 0x18, 0x00, 0x18, 0x18, 0x18, 0x00,
            0x07, 0x0C, 0x0C, 0x38, 0x0C, 0x0C, 0x07, 0x00,
            0x6E, 0x3B, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    public enum PanelType {
        EPD210(296, 128, 4736),
        EPD302(416, 240, 24960),
        EPD303(416, 240, 24960),
        EPD304(416, 240, 24960);

        private final int width;
        private final int height;
        private int size;

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getSize() {
            return size;
        }

        PanelType(int width, int height, int size) {
            this.width = width;
            this.height = height;
            this.size = size;
        }
    }


    public enum EinkImageTemplate {
        EINK_IMAGE_BLACK,
        EINK_IMAGE_WHITE,
        EINK_IMAGE_VERTICAL_0,
        EINK_IMAGE_VERTICAL_1,
        EINK_IMAGE_HORIZOTAL_0,
        EINK_IMAGE_HORIZOTAL_1,
        EINK_IMAGE_RANDOM
    } ;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPages() { return pages; }

    public byte[] getData() {
        return data;
    }

    public int getLz4size() {
        return lz4size;
    }

    public byte[] getLz4data() {
        return lz4data;
    }

    private int width;
    private int height;
    private int pages;
    private byte[] data;
    private int lz4size;
    private byte[] lz4data;

    public EinkImage(int width, int height, int pages, InputStream inStream) {
        this.width = width;
        this.height = height;
        this.pages = pages;

        data = new byte[width*height/8*pages];  // bw & r (2 pages)

        try {
//            data = new byte[inStream.available()];
            inStream.read(data, 0, width*height/8*pages);
        } catch (IOException e) {
            data = null;
        }
    }

    public EinkImage(int width, int height, int pages, Bitmap bitmap) {
        this.width = width;
        this.height = height;
        this.pages = pages;
        Matrix matrix = new Matrix();

        if(bitmap.isRecycled()) {
            bitmap =  Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
        }

        int count = width*height/8;
        data = new byte[count*pages];  // bw & r (2 pages)
        //int index = 0;

        ImageConverter imageConverter = new ImageConverter();
        if (count == PanelType.EPD210.getSize()) {
            data = imageConverter.img_forEPD29(bitmap);
        } else {
            data = imageConverter.rgb2_forEPD37(imageConverter.img2rgb_forEPD37(bitmap));
        }

//        for(int j=0;j<width;j++) {
//            for(int i=0;i<height;i+=8) {
//                int v = 0;
//                for(int k=0;k<8;k++) {
//                    int color = bitmap.getPixel(width-j-1, i+k);
//                    // convert to BW
//                    v *= 2;
//                    if(ConvertPixel(color)) {
//                        v |= 1;
//                    }
//                }
//                data[index] = (byte) (v&0xFF);
//                if(pages!=1)
//                    data[index+count] = data[index];
//                index++;
//            }
//        }
    }

    public EinkImage(int width, int height, int pages, Bitmap bitmap, int lz4flag, int lz4packsize, PanelType type) {
        Log.d(TAG, "EinkImage: width : ["+width+"]  height : ["+height+"]  pages : ["+pages+"]  lz4flag : ["+lz4flag+"]  lz4packsize : ["+lz4packsize+"] ");
        this.width = width;
        this.height = height;
        this.pages = pages;
        Matrix matrix = new Matrix();
        ImageConverter imageConverter = new ImageConverter();

        if(bitmap.isRecycled()) {
            bitmap =  Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
        }

//        bitmap = imageConverter.resizeImage(bitmap, width, height);

        int count = width*height/8;
        data = new byte[count * pages];  // bw & r (2 pages)
        lz4data = new byte[count * pages];

        //int index = 0;
        if (type == PanelType.EPD210) {
            data = imageConverter.img_forEPD29(bitmap);
        } else if (type == PanelType.EPD302) {
            data = imageConverter.rgb2_forEPD37(imageConverter.img2rgb_forEPD37(bitmap));
//            data = imageConverter.img_forEPD302(bitmap);
        } else if (type == PanelType.EPD303) {
            data = imageConverter.rgb2_forEPD37(imageConverter.img2rgb_forEPD37(bitmap));
        } else if (type == PanelType.EPD304) {
            data = imageConverter.img_forEPD304(bitmap);
        }

        if (lz4flag == 0)
        {
            lz4data = new byte[1];
            lz4data[0] = 0x00;
            lz4size = 0;
        }
        else
        {
            int page1 = 1;
            Lz4comp_segment(width, height, page1, lz4packsize);
            if (pages > 1)
            {
                Lz4comp_segment(width, height, pages, lz4packsize);
            }
        }

    }

    public EinkImage(int width, int height, int pages) {
        this.width = width;
        this.height = height;
        this.pages = pages;

        int count = width * height / 8;
        data = new byte[count * pages];  // bw & r (2 pages)
        for (int i = 0; i < count; i++) {
            data[i] = 0;
        }
    }

    public EinkImage(int width, int height, int pages, EinkImageTemplate type) {
        this.width = width;
        this.height = height;
        this.pages = pages;

        Random rand = new Random();

        int count = width * height / 8;
        data = new byte[count * pages];  // bw & r (2 pages)
        int hcount = height / 8;

        for (int i = 0; i < count; i++) {
            switch (type) {
                case EINK_IMAGE_BLACK:
                    data[i] = (byte) 0x00;
                    if (pages != 1)
                        data[i + count] = (byte) 0x00;
                    break;

                case EINK_IMAGE_WHITE:
                    data[i] = (byte) 0xFF;
                    if (pages != 1)
                        data[i + count] = (byte) 0xFF;
                    break;

                case EINK_IMAGE_RANDOM:
                    data[i] = (byte) (rand.nextInt(0x100) & 0xFF);
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;

                case EINK_IMAGE_HORIZOTAL_0:
                    data[i] = (byte) 0x55;
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;

                case EINK_IMAGE_HORIZOTAL_1:
                    data[i] = (byte) 0xAA;
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;

                case EINK_IMAGE_VERTICAL_0:
                    if ((i / hcount) % 2 == 0) {
                        data[i] = (byte) 0xFF;
                    } else {
                        data[i] = (byte) 0x00;
                    }
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;

                case EINK_IMAGE_VERTICAL_1:
                    if ((i / hcount) % 2 == 1) {
                        data[i] = (byte) 0xFF;
                    } else {
                        data[i] = (byte) 0x00;
                    }
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;
            }
        }
    }

    public EinkImage(int width, int height, int pages, EinkImageTemplate type, int lz4flag, int lz4packsize) {
        this.width = width;
        this.height = height;
        this.pages = pages;

        Random rand = new Random();

        int count = width * height / 8;
        data = new byte[count * pages];  // bw & r (2 pages)
        lz4data = new byte[count * pages];
        int hcount = height / 8;

        for (int i = 0; i < count; i++) {
            switch (type) {
                case EINK_IMAGE_BLACK:
                    data[i] = (byte) 0x00;
                    if (pages != 1)
                        data[i + count] = (byte) 0x00;
                    break;

                case EINK_IMAGE_WHITE:
                    data[i] = (byte) 0xFF;
                    if (pages != 1)
                        data[i + count] = (byte) 0xFF;
                    break;

                case EINK_IMAGE_RANDOM:
                    data[i] = (byte) (rand.nextInt(0x100) & 0xFF);
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;

                case EINK_IMAGE_HORIZOTAL_0:
                    data[i] = (byte) 0x55;
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;

                case EINK_IMAGE_HORIZOTAL_1:
                    data[i] = (byte) 0xAA;
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;

                case EINK_IMAGE_VERTICAL_0:
                    if ((i / hcount) % 2 == 0) {
                        data[i] = (byte) 0xFF;
                    } else {
                        data[i] = (byte) 0x00;
                    }
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;

                case EINK_IMAGE_VERTICAL_1:
                    if ((i / hcount) % 2 == 1) {
                        data[i] = (byte) 0xFF;
                    } else {
                        data[i] = (byte) 0x00;
                    }
                    if (pages != 1)
                        data[i + count] = data[i];
                    break;
            }
        }

        if (lz4flag == 0)
        {
            lz4data = new byte[1];
            lz4data[0] = 0x00;
            lz4size = 0;
        }
        else
        {
            int page1 = 1;
            Lz4comp_segment(width, height, page1, lz4packsize);
            if (pages > 1)
            {
                Lz4comp_segment(width, height, pages, lz4packsize);
            }
        }
    }

    private void Lz4comp_segment(int img_width, int img_height, int page, int packsize) {
        int packet_size = packsize, final_packet = 0, finflag = 0;
        int compressSize = 0;
        int sz, i, lzsum, datasz, stpos, packno, ipadnb, pknmb;


        // get minimum work buffer
        datasz = (img_width * img_height) / 8;
        ipadnb = ((datasz / packsize) + 1) * 2;
        byte[] buffer = new byte[LZ4Factory.fastestInstance().fastCompressor().maxCompressedLength(packet_size)];
        pknmb = ((datasz / packsize) + 1);
        compressSize = buffer.length * pknmb;
        byte[] tmplz4_data = new byte[LZ4Factory.fastestInstance().fastCompressor().maxCompressedLength(compressSize) + ipadnb];


        boolean Lzst = true;
        // get buffers pointers
        packno = stpos = lzsum = sz = 0;
        if (page == 1)
            i = 0;
        else
            i = (img_width * img_height) / 8;

        while (Lzst) {
            // compress
            if (finflag == 0) {
                sz = LZ4Factory.fastestInstance().fastCompressor().compress(data, i, packet_size, buffer, 0, buffer.length);
            } else {
                sz = LZ4Factory.fastestInstance().fastCompressor().compress(data, i, final_packet, buffer, 0, buffer.length);
            }

            if (lzsum == 0) {
                tmplz4_data[stpos] = (byte) (sz);
                tmplz4_data[stpos + 1] = (byte) (sz >> 8);
            } else {
                tmplz4_data[stpos + 1] = (byte) (sz);
                tmplz4_data[stpos + 2] = (byte) (sz >> 8);
            }

            if (lzsum == 0) {
                System.arraycopy(buffer, 0, tmplz4_data, (stpos + 2), sz);
            } else {
                System.arraycopy(buffer, 0, tmplz4_data, (stpos + 3), sz);
            }

            lzsum += sz + 2;
            stpos = lzsum;

            if ((datasz) > packet_size) {
                i += packet_size;
                datasz -= packet_size;
                if (datasz <= packet_size) {
                    final_packet = datasz;
                    finflag = 1;
                }
            } else if ((datasz) <= packet_size) {
                i += datasz;
                datasz -= datasz;
                if (datasz == 0) {
                    tmplz4_data[stpos + 1] = 0x0D;
                    tmplz4_data[stpos + 2] = 0x0A;
                    lzsum += 2;
                    Lzst = false;
                }
            }
        }

        if (page == 1) {
            int page1 = (img_width * img_height) / 8;
            if (lzsum < page1)
                System.arraycopy(tmplz4_data, 0, lz4data, 0, lzsum + 1);
        } else {
            if ((lz4size + lzsum) < data.length)
                System.arraycopy(tmplz4_data, 0, lz4data, lz4size, lzsum + 1);
        }
        lz4size += lzsum + 1;
    }

    // Draw simple text on EinkImage
    public void DrawText(int x, int y, String s) {
        for(char c: s.toCharArray()) {
            byte b = (byte) (c & 0x7F);
            for(int i=0;i<8;i++) {
                byte fd = font8x8[(int)b*8+i];
                for(int j=0;j<8;j++) {
                    // x+j, y+i
                    int color = (fd & 0x1)!=0 ? 0:1;
                    DrawPixel(x+j,y+i, color);
                    fd >>= 1;
                }
            }
            x += 8;
        }
    }

    public void DrawText5x5Digit(int x, int y, String s) {
        for(char c: s.toCharArray()) {
            byte b = (byte) (c & 0x7F);
            if(b>=65)
                b = (byte) (b - 65 + 10);
            else
                b = (byte) (b-48);
            for(int i=0;i<5;i++) {
                byte fd = font5x5[(int)b*5+i];
                for(int j=0;j<6;j++) {
                    // x+j, y+i
                    int color = (fd & 0x80)!=0 ? 0:1;
                    DrawPixel(x+j,y+i, color);
                    fd <<= 1;
                }
            }
            x += 6;
        }
    }

    public void DrawPixel(int x, int y, int color) {
        int count = width*height/8;
        int off_y = y / 8;
        int mod_y = y % 8;
        int off_x = (height/8)*(width-x-1);
        int off = off_x+off_y;
        byte b = data[off];
        if(color==0) {
            // black
            b = (byte) (b & ~(1<<(7-mod_y)));
            data[off] = b;
            if(pages==2) {
                data[off+count] = b;
            }
        } else {
            // white
            b = (byte) (b | (1<<(7-mod_y)));
            data[off] = b;
            if(pages==2) {
                data[off+count] = b;
            }
        }
    }

}
