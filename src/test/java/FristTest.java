import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FristTest {
//    Frame 4: 230 bytes on wire (1840 bits), 230 bytes captured (1840 bits)
//    Ethernet II, Src: 00:00:00_00:00:00 (00:00:00:00:00:00), Dst: 00:00:00_00:00:00 (00:00:00:00:00:00)
//    Internet Protocol Version 4, Src: 127.0.0.1, Dst: 127.0.0.1
//    Transmission Control Protocol, Src Port: 38780, Dst Port: 8765, Seq: 1, Ack: 1, Len: 164
//    Hypertext Transfer Protocol
//    POST /a HTTP/1.1\r\n                18
//    Host: localhost:8765\r\n
//    User-Agent: curl/8.0.1\r\n
//    Accept: */*\r\n
//    Content-Length: 16\r\n
//    Content-Type: application/x-www-form-urlencoded\r\n
//    \r\n
//    hell11111o world" = "
//    [Full request URI: http://localhost:8765/a]
//    [HTTP request 1/1]
//    File Data: 16 bytes
//HTML Form URL Encoded: application/x-www-form-urlencoded
//    Form item: ""

    @Order(1)
    @Test
    public void test_1_Http() {
        String s = "POST /a HTTP/1.1\r\n" +
                "Host: localhost:8765\r\n" +
                "User-Agent: curl/8.0.1\r\n" +
                "Accept: */*\r\n" +
                "Content-Length: 16\r\n" +
                "Content-Type: application/x-www-form-urlencoded\r\n" +
                "\r\n" +
                "1234567890123456";
        ByteBuf in = Unpooled.wrappedBuffer(s.getBytes(StandardCharsets.UTF_8));
        List<Object> out = new ArrayList<>();
        decodeHttp(in,out);
        printOut(out);


    }
    @Test
    private void test_2_redis() {
        String s1 = "*5\r\n" +
                ":1\r\n" +
                ":2\r\n" +
                ":3\r\n" +
                ":4\r\n" +
                "$5\r\n" +
                "hello\r\n";
        ByteBuf buf = Unpooled.wrappedBuffer(s1.getBytes(StandardCharsets.UTF_8));
        List<Object> out = new ArrayList<>();

        redisDecode(buf, out);
        printOut(out);
    }
    private static void redisDecode(ByteBuf in, List<Object> out) {

        while (true) {
            if (!in.isReadable()) {
                break;
            }

            byte first = in.readByte();
            switch (first) {
                case '*':
                case '+':
                case '-':
                case ':': {
                    ByteBuf buf = findSliceRead(in,processor);
                    if (null != buf) {
                        out.add(buf.toString(StandardCharsets.UTF_8));
                    }
                }
                break;
                case '$': {
                    ByteBuf buf = findSliceRead(in,processor);
                    lastLen = Integer.parseInt(buf.toString(StandardCharsets.UTF_8));
                    out.add(lastLen);
                    ByteBuf buf1 = sliceRead(in,lastLen);
                    out.add(buf1.toString(StandardCharsets.UTF_8));
                }
                break;
                default:
                    break;
            }
        }

    }

    enum State {
        START,
        BODY,
        NEXT,
    }
    private static void printOut(List<Object> out) {
        for (Object o : out) {
            System.out.println(o);
        }
    }
    private static int lastLen = -1;
    private static ByteProcessor processor = new ByteProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value != 13 && value != 10;
        }
    };

    private static ByteBuf sliceRead(ByteBuf in,int len){
        if (in.isReadable()) {
            ByteBuf buf = in.slice(in.readerIndex(), lastLen);
            in.skipBytes(lastLen + 2);
            return buf;
        }
        return null;
    }

    private static ByteBuf findSliceRead(ByteBuf in,ByteProcessor processor){
        int index = in.forEachByte(processor);
        if (index > -1) {
            int len = index - in.readerIndex();
            ByteBuf buf = in.slice(in.readerIndex(), len);
            in.skipBytes(len + 2);
            return buf;
        }
        return null;
    }


    private static void decodeHttp(ByteBuf in, List<Object> out) {
        State state = State.START;
        Queue<String> lines = new LinkedList<>();
        int contentLen = 0;

        while (in.isReadable()) {
            if (state == State.START) {
                ByteBuf buf = findSliceRead(in,processor);
                if (buf != null) {
                    String line = buf.toString(StandardCharsets.UTF_8);
                    if (line.isBlank())  {
                        state = State.BODY;
                    } else {
                        if (line.startsWith("Content-Length")) {
                            contentLen = Integer.parseInt(line.substring("Content-Length".length()+2));
                        }
                        lines.add(line);
                    }

                }
            } else if (state == State.BODY) {
                byte[] body = new byte[contentLen];
                in.readBytes(body);
                lines.add(new String(body));
            }


        }


    }

}
