package cn.navclub.nes4j.bin.log.formatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

public class NFormatter extends Formatter {
    private final Pattern pattern;
    private final SimpleDateFormat format;

    public NFormatter() {
        this.pattern = Pattern.compile("\\{}");
        this.format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @Override
    public String format(LogRecord record) {
        var str = String.format("[%s [%s] [%-7s] [%s]",
                this.format.format(new Date()),
                Thread.currentThread().getName(),
                record.getLevel().getName(),
                record.getLoggerName()
        );
        var message = record.getMessage();
        var params = record.getParameters();
        var length = params == null ? 0 : params.length;

        var index = 0;
        var offset = 0;
        var sb = new StringBuilder(message);
        var matter = pattern.matcher(message);
        while (matter.find() && index < length) {
            var start = matter.start();
            var end = matter.end();
            var param = record.getParameters()[index];
            if (param == null) {
                param = "nil";
            }
            var value = param.toString();
            sb.replace(start + offset, end + offset, value);
            offset += (value.length() - 2);
            index++;
        }
        str = String.format("%s %s", str, sb);

        if (record.getLevel() == Level.SEVERE) {
            var e = record.getThrown();
            if (e != null) {
                str += "\n";
                str += throwable2Str(e);
            }
        }
        str += "\n";
        return str;
    }

    private String throwable2Str(Throwable throwable) {
        try (
                var sw = new StringWriter();
                var pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return throwable2Str(new RuntimeException("Log exception parser error!"));
        }
    }
}
