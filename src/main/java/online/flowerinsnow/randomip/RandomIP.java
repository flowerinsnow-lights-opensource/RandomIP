package online.flowerinsnow.randomip;

import online.flowerinsnow.saussureautils.io.CopyOption;
import online.flowerinsnow.saussureautils.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.regex.Pattern;

public class RandomIP {
    public static void main(String[] args) {
        Path jarFile;
        try {
            jarFile = new File(RandomIP.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        Path ipsFile = jarFile.resolveSibling("ips.txt"); // IP列表文件
        if (!ipsFile.toFile().isFile()) {
            try {
                //noinspection DataFlowIssue
                IOUtils.copy(RandomIP.class.getResourceAsStream("/ips.txt"), ipsFile, CopyOption.CLOSE_INPUT);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        final HashSet<String> ipList = new HashSet<>();
        BufferedReader br = null;
        final String regex0to255 = "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])";
        final String regex0to32 = "([0-9]|[1-2][0-9]|3[0-2])";
        final Pattern ipv4 = Pattern.compile("(" + regex0to255 + "\\.){3}" + regex0to255 + "(/" + regex0to32 + ")");
        try {
            br = Files.newBufferedReader(ipsFile, StandardCharsets.UTF_8);
            String line;
            while ((line = br.readLine()) != null) {
                if (ipv4.matcher(line).matches()) {
                    ipList.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            IOUtils.closeQuietly(br);
        }

        final Random random = new Random();

        ipList.forEach(ip -> {
            boolean[] bits = new boolean[32]; // 用于存放32位 false=0 1=true
            short[] bytes = new short[4]; // 4个点分十进制
            // 计次4次，读取到'.'或者'/'就将刚刚经过的数字保存
            int times = 0;
            int previousIndex = -1;
            for (int index = 0; index < ip.length(); index++) {
                if (ip.charAt(index) == '.' || ip.charAt(index) == '/') {
                    bytes[times++] = (short) Integer.parseUnsignedInt(ip.substring(previousIndex + 1, index));
                    previousIndex = index;
                }
            }
            // 获取子网掩码长度
            int subnetMaskLen = Integer.parseInt(ip.substring(ip.lastIndexOf("/") + 1));

            // 将字节解析到位
            int bitIndex = 0;
            for (short aByte : bytes) {
                for (int j = 7; j >= 0; j--) {
                    bits[bitIndex++] = ((aByte >>> j) & 0x1) == 1;
                }
            }

            // 取100个随机IP
            for (int count = 0; count < 100; count++) {
                boolean[] copy = new boolean[bits.length];
                System.arraycopy(bits, 0, copy, 0, bits.length);
                // 从子网掩码位开始，后面每一位都可以随机
                for (int i = subnetMaskLen; i < bits.length; i++) {
                    copy[i] = random.nextBoolean();
                }
                System.out.println(toIPString(copy));
            }
        });
    }

    private static String toIPString(boolean[] bits) {
        if (bits.length != 32) {
            throw new IllegalArgumentException();
        }
        // 将位解析到字节
        short[] bytes = new short[4];
        for (int i = 0; i < bytes.length; i++) {
            short s = 0;
            for (int j = 7; j >= 0; j--) {
                s |= ( bits[i * 8 + (7 - j)] ) ? (1 << j) : 0;
            }
            bytes[i] = s;
        }
        return bytes[0] + "." + bytes[1] + "." + bytes[2] + "." + bytes[3];
    }
}
