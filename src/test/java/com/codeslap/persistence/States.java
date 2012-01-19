package com.codeslap.persistence;

import org.junit.Test;

public class States {
    @Test
    public void test() {
        String[] lines = PEPE.split("\n");
        float min = Float.MAX_VALUE;
        String state = null;
        for (String line : lines) {
            String[] parts = line.split(",");
//            System.out.println("<item state=\"" + parts[0] + "\" lat=\"" + parts[1] + "\" lon=\"" + parts[2] + "\">");
            float distance = calculateDistance(39.834196, -84.807075, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
            if (distance < min) {
                min = distance;
                state = parts[0];
            }
        }

        System.out.printf(":::: closet %s, distance %s%n", state, min);
    }

    float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return (float) (6.371 * c);
    }


    private static final String PEPE = "AK,61.3850,-152.2683\n" +
            "AL,32.7990,-86.8073\n" +
            "AR,34.9513,-92.3809\n" +
            "AS,14.2417,-170.7197\n" +
            "AZ,33.7712,-111.3877\n" +
            "CA,36.1700,-119.7462\n" +
            "CO,39.0646,-105.3272\n" +
            "CT,41.5834,-72.7622\n" +
            "DC,38.8964,-77.0262\n" +
            "DE,39.3498,-75.5148\n" +
            "FL,27.8333,-81.7170\n" +
            "GA,32.9866,-83.6487\n" +
            "HI,21.1098,-157.5311\n" +
            "IA,42.0046,-93.2140\n" +
            "ID,44.2394,-114.5103\n" +
            "IL,40.3363,-89.0022\n" +
            "IN,39.8647,-86.2604\n" +
            "KS,38.5111,-96.8005\n" +
            "KY,37.6690,-84.6514\n" +
            "LA,31.1801,-91.8749\n" +
            "MA,42.2373,-71.5314\n" +
            "MD,39.0724,-76.7902\n" +
            "ME,44.6074,-69.3977\n" +
            "MI,43.3504,-84.5603\n" +
            "MN,45.7326,-93.9196\n" +
            "MO,38.4623,-92.3020\n" +
            "MP,14.8058,145.5505\n" +
            "MS,32.7673,-89.6812\n" +
            "MT,46.9048,-110.3261\n" +
            "NC,35.6411,-79.8431\n" +
            "ND,47.5362,-99.7930\n" +
            "NE,41.1289,-98.2883\n" +
            "NH,43.4108,-71.5653\n" +
            "NJ,40.3140,-74.5089\n" +
            "NM,34.8375,-106.2371\n" +
            "NV,38.4199,-117.1219\n" +
            "NY,42.1497,-74.9384\n" +
            "OH,40.3736,-82.7755\n" +
            "OK,35.5376,-96.9247\n" +
            "OR,44.5672,-122.1269\n" +
            "PA,40.5773,-77.2640\n" +
            "PR,18.2766,-66.3350\n" +
            "RI,41.6772,-71.5101\n" +
            "SC,33.8191,-80.9066\n" +
            "SD,44.2853,-99.4632\n" +
            "TN,35.7449,-86.7489\n" +
            "TX,31.1060,-97.6475\n" +
            "UT,40.1135,-111.8535\n" +
            "VA,37.7680,-78.2057\n" +
            "VI,18.0001,-64.8199\n" +
            "VT,44.0407,-72.7093\n" +
            "WA,47.3917,-121.5708\n" +
            "WI,44.2563,-89.6385\n" +
            "WV,38.4680,-80.9696\n" +
            "WY,42.7475,-107.2085";
}
