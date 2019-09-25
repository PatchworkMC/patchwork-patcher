package net.coderbot.patchwork.logging;

import org.fusesource.jansi.Ansi;

public enum LogLevel {
    /*
     * Ansi.ansi().fgBright(Ansi.Color.BLACK).a("[").fgBright(Ansi.Color.MAGENTA).a("@").fgBright(Ansi.Color.BLACK).a("]").reset().toString()
     * Ansi.ansi().fgBright(Ansi.Color.BLACK).a("[").fg(Ansi.Color.YELLOW).a("D").fgBright(Ansi.Color.BLACK).a("]").reset().toString()
     * Ansi.ansi().fgBright(Ansi.Color.BLACK).a("[").fgBright(Ansi.Color.BLUE).a("*").fgBright(Ansi.Color.BLACK).a("]").reset().toString()
     * Ansi.ansi().fgBright(Ansi.Color.BLACK).a("[").fgBright(Ansi.Color.YELLOW).a("!").fgBright(Ansi.Color.BLACK).a("]").reset().toString()
     * Ansi.ansi().fgBright(Ansi.Color.BLACK).a("[").fgBright(Ansi.Color.RED).a("-").fgBright(Ansi.Color.BLACK).a("]").reset().toString()
     */

    TRACE(5000),
    DEBUG(10000),
    INFO(20000),
    WARN(30000),
    ERROR(40000),
    FATAL(50000);

    private final int numericalLevel;

    LogLevel(int numericalLevel) {
        this.numericalLevel = numericalLevel;
    }

    public int numerical() {
        return numericalLevel;
    }

    public boolean includes(LogLevel other) {
        return this.numericalLevel <= other.numericalLevel;
    }
}
