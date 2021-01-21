package edu.salk.brat.layout;


import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IJRoiImporter {
    private List<int[]> roiPositions;

    public List<int[]> getRoiPositions() {
        return roiPositions;
    }

    public void openZip(String path) {
        roiPositions = new ArrayList<>();

        ZipInputStream in = null;
        ByteArrayOutputStream out = null;

        try {
            in = new ZipInputStream(new FileInputStream(path));
            byte[] buf = new byte[16]; //we read only the first 16 bytes, all needed information should be there

            for(ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                String name = entry.getName();
                if (name.endsWith(".roi")) {
                    out = new ByteArrayOutputStream();

                    int len = in.read(buf);
                    if(len == buf.length) {
                        out.write(buf, 0, len);
                    }
                    else {
                        continue;
                    }

                    out.close();
                    byte[] bytes = out.toByteArray();
                    SimpleRoiDecoder rd = new SimpleRoiDecoder(bytes);
                    int[] roiPosition = rd.decode();
                    if (roiPosition != null) {
                        this.roiPositions.add(roiPosition);
                    }
                }
            }

            in.close();
        } catch (IOException var24) {
            System.out.println(var24.toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException var23) {
                    ;
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException var22) {
                    ;
                }
            }
        }
    }

    public static void main(String[] args) {
        IJRoiImporter ri = new IJRoiImporter();
        ri.openZip("/home/crisoo/RoiSet.zip");
    }
}

class SimpleRoiDecoder {
    /*
    Code taken from IJ RoiDecoder
    extracts only Roi bounds from PointRois
    (this was necessary because IJ's RoiDecoder terminated with an IOException
     */

    private byte[] data;

    SimpleRoiDecoder (byte[] data) {
        this.data = data;
    }

    int[] decode() {
        int[] position = null;
        if (this.getByte(0) == 73 && this.getByte(1) == 111) {
            if (getByte(6) == 10) { //check for PointRoi
                int top = this.getShort(8);
                int left = this.getShort(10);
                position = new int[]{left, top};
            }
        }
        return position;
    }

    private int getByte(int base) {
        return this.data[base] & 255;
    }

    private int getShort(int base) {
        int b0 = this.data[base] & 255;
        int b1 = this.data[base + 1] & 255;
        int n = (short)((b0 << 8) + b1);
        if (n < -5000) {
            n = (b0 << 8) + b1;
        }

        return n;
    }

}