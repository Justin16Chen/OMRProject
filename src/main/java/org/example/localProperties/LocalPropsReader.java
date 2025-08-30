package org.example.localProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class LocalPropsReader {
    private static final boolean usePrespecifiedChamberMonitorSize;
    private static double chamberMonitorWidthCm, chamberMonitorHeightCm;
    static {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("local.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            System.out.println("FAILED TO OPEN LOCAL.PROPERTIES FILE");
            e.printStackTrace();
        }
        String useSize = properties.getProperty("use.prespecified.chamber.monitor.size");
        if (useSize == null)
            throw new IllegalStateException("LOCAL.PROPERTIES FILE IS WRONG - use.prespecified.chamber.monitor.size cannot be found");
        usePrespecifiedChamberMonitorSize = useSize.equals("true");
        if (usePrespecifiedChamberMonitorSize) {
            String width = properties.getProperty("chamber.monitor.width.cm");
            String height = properties.getProperty("chamber.monitor.height.cm");
            try {
                chamberMonitorWidthCm = Double.parseDouble(width);
                chamberMonitorHeightCm = Double.parseDouble(height);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("LOCAL.PROPERTIES FILE IS WRONG - chamber.monitor.width.cm or chamber.monitor.height.cm key is not a number");
            }
        }
    }
    public static boolean shouldUsePrespecifiedChamberMonitorSize() { return usePrespecifiedChamberMonitorSize; }
    public static double getChamberMonitorWidthCm() { return chamberMonitorWidthCm; }
    public static double getChamberMonitorHeightCm() { return chamberMonitorHeightCm; }
}
