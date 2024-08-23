// On-screen Display
// for the Build Engine
// by Jonathon Fowler (jf@jonof.id.au)
//
// This file is part of BuildGDX.
// Copyright (C) 2023-2024 Alexander Makarov-[M210] (m210-2007@mail.ru)
//
// BuildGDX is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// BuildGDX is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with BuildGDX.  If not, see <http://www.gnu.org/licenses/>.

package ru.m210projects.Build.osd;

import com.badlogic.gdx.Input;
import ru.m210projects.Build.settings.GameKeys;
import ru.m210projects.Build.StringUtils;
import ru.m210projects.Build.Types.collections.LinkedList;
import ru.m210projects.Build.Types.collections.ListNode;
import ru.m210projects.Build.input.GameKey;
import ru.m210projects.Build.input.InputListener;
import ru.m210projects.Build.osd.commands.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.m210projects.Build.Engine.MAXPALOOKUPS;
import static ru.m210projects.Build.Render.DummyRenderer.DUMMY_RENDERER;

public class Console implements InputListener {

    private static final int MAX_LINES = 512;
    private static final OsdCommand UNKNOWN_COMMAND = new UnknownCommand();
    public static Console out = new Console();

    final Map<String, OsdCommand> osdVars;
    OsdFunc func = new DefaultOsdFunc(DUMMY_RENDERER);
    // TODO сделать OSDString сегментами (сегмент цвета и кусок текста)
    private final LinkedList<OsdString> osdTextList = new LinkedList<>();
    protected final OsdCommandPrompt prompt;

    protected final CommandFinder finder;
    /**
     * width of onscreen display in text columns
     */
    int osdCols = 65;
    int osdTextScale = 65536;
    /**
     * visible row count when moving (from 0 to osdRow)
     */
    int osdRowsCur = -1;
    private boolean osdDraw = false;
    /**
     * position next character will be written at
     */
    private int osdCharacterPos = 0;
    /**
     * topmost visible line number
     */
    private ListNode<OsdString> osdHead;
    /**
     * # lines of the buffer that are visible
     */
    private int osdRows = 20;
    /**
     * maximum number of lines which can fit on the screen
     */
    private int osdMaxRows = 20;
    private int osdScroll = 0;
    private long osdScrTime = 0;
    private OsdColor osdTextPal = OsdColor.DEFAULT;
    private int osdTextShade = 0;
    private ConsoleLogger logger;

    public Console() {
        this.osdVars = new HashMap<>();
        this.prompt = new OsdCommandPrompt(512, 32);
        prompt.registerDefaultCommands(this);
        registerDefaultCommands();
        finder = new CommandFinder(osdVars);

        prompt.setActionListener(input -> {
            if (!input.isEmpty()) {
                dispatch(input);
            }
            setFirstLine();
        });
    }

    protected void registerDefaultCommands() {
        registerCommand(new OsdCallback("osd_color_test", "", args -> {
            int len = MAXPALOOKUPS;

            if (args.length != 0 && StringUtils.isNumeric(args[0])) {
                len = Integer.parseInt(args[0]);
            }

            for (int i = 0; i < len; i++) {
                Console.out.println(String.format("pal%d: ^%dThe quick brown fox jumps over the lazy dog", i, i));
            }
            return CommandResponse.SILENT_RESPONSE;
        }));

        registerCommand(new OsdCommand("help", "lists all registered functions, cvars and aliases") {
            @Override
            public CommandResponse execute(String[] argv) {
                List<String> commands = osdVars.keySet().stream().filter(e -> !e.equals(getName())).sorted().collect(Collectors.toList());
                if (!commands.isEmpty()) {
                    printCommandList(commands, "Symbol listing:", String.format("Found %d symbols", commands.size()));
                    return CommandResponse.SILENT_RESPONSE;
                }
                return CommandResponse.DESCRIPTION_RESPONSE;
            }
        });
        registerCommand(new OsdValueRange("osdtextpal", "osdtextpal: sets the palette of the OSD text", 0, MAXPALOOKUPS - 1) {
            @Override
            public float getValue() {
                return osdTextPal.getPal();
            }

            @Override
            protected void setCheckedValue(float value) {
                osdTextPal = OsdColor.findColor((int) value);
            }
        });
        registerCommand(new OsdValueRange("osdtextshade", "osdtextshade: sets the shade of the OSD text", 0, 255) {
            @Override
            public float getValue() {
                return osdTextShade;
            }

            @Override
            protected void setCheckedValue(float value) {
                osdTextShade = (int) value;
            }
        });
        registerCommand(new OsdValue("osdtextscale", "osdtextscale: sets the OSD text scale", value -> value >= 0.5f) {
            @Override
            public float getValue() {
                return osdTextScale / 65536.0f;
            }

            @Override
            protected void setCheckedValue(float value) {
                setOsdTextScale(value);
            }
        });
        registerCommand(new OsdValue("osdrows", "osdrows: sets the number of visible lines of the OSD", value -> (value >= 0) && (value <= osdMaxRows)) {
            @Override
            public float getValue() {
                return osdRows;
            }

            @Override
            protected void setCheckedValue(float value) {
                osdRows = (int) value;
                stopMoving();
            }
        });
    }

    public void setFunc(OsdFunc func) {
        this.func = func;
        this.osdRows = 20;
    }

    public void registerCommand(OsdCommand cmd) {
        cmd.setParent(this);

        String name = cmd.getName();
        if (name.isEmpty()) {
            throw new RegisterException("Can't register null command");
        }

        if (Character.isDigit(name.charAt(0))) {
            throw new RegisterException(String.format("first character of command name \"%s\" must not be a numeral", name));
        }

        for (char ch : name.toCharArray()) {
            if (!Character.isLetterOrDigit(ch) && ch != '_') {
                throw new RegisterException(String.format("Illegal character in command name \"%s\"", name));
            }
        }

//        if (osdVars.containsKey(name)) {
//            Console.out.println(String.format("Warning!: Command name \"%s\" is already registered", name), OsdColor.RED);
//            // throw new RegisterException(String.format("Command name \"%s\" is already registered", name));
//        }

        osdVars.put(name, cmd);
    }

    public float getValue(String cmd) {
        OsdCommand command = osdVars.getOrDefault(cmd, UNKNOWN_COMMAND);
        if (command instanceof OsdValue) {
            return ((OsdValue) command).getValue();
        }
        return Float.NaN;
    }

    public boolean setValue(String cmd, float value) {
        OsdCommand command = osdVars.getOrDefault(cmd, UNKNOWN_COMMAND);
        if (command instanceof OsdValue) {
            return ((OsdValue) command).setValue(value);
        }
        return false;
    }

    public OsdCommandPrompt getPrompt() {
        return prompt;
    }

    void dispatch(String text) {
        try {
            if (text.isEmpty()) {
                return;
            }

            String[] argv = text.split(" ");
            OsdCommand command = UNKNOWN_COMMAND;
            if (argv.length > 0) {
                command = osdVars.getOrDefault(argv[0], UNKNOWN_COMMAND);
                argv = Arrays.copyOfRange(argv, 1, argv.length);
            }

            switch (command.execute(argv)) {
                case OUT_OF_RANGE:
                    println(String.format("\"%s\" value out of range", command.getName()));
                    break;
                case BAD_ARGUMENT_RESPONSE:
                    println(String.format("\"%s\" wrong value \"%s\"", command.getName(), text.substring(command.getName().length()).trim()));
                    break;
                case SILENT_RESPONSE:
                    return;
                case OK_RESPONSE:
                    println(text);
                    break;
                case DESCRIPTION_RESPONSE:
                    println(command.getDescription());
                    break;
                case UNKNOWN_RESPONSE:
                    if (func.textHandler(text)) {
                        return;
                    }

                    println(String.format("\"%s^O\" is not a valid command or cvar", text), OsdColor.RED);
                    break;
            }
        } catch (Exception e) {
            println(e.toString(), OsdColor.RED);

            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append("\t").append(element.toString());
                sb.append("\r\n");
            }
            logger.write(sb.toString());
        }
    }

    public boolean isMoving() {
        return (osdRowsCur != -1 && osdRowsCur != osdRows);
    }

    public boolean isShowing() {
        return osdDraw;
    }

    public void revalidate() {
        osdCols = func.getcolumnwidth(osdTextScale);
        osdRows = Math.max(osdRows, 1);
        osdMaxRows = Math.max(1, func.getrowheight(osdTextScale) - 2);
        if (osdRows < 0 || osdRows > osdMaxRows) {
            osdRows = osdMaxRows;
        }

        if (osdDraw) {
            osdRowsCur = osdRows;
        }

        prompt.onResize();
        osdCharacterPos = 0;
        setFirstLine();
    }

    public void draw() {
        if (osdRowsCur == 0) {
            osdDraw = !osdDraw;
        }

        if (osdRowsCur == osdRows || osdRowsCur == osdMaxRows) {
            osdScroll = 0;
        } else {
            if ((osdRowsCur < osdRows && osdScroll == 1) || osdRowsCur < -1) {
                long j = (getTicks() - osdScrTime);
                while (j > -1) {
                    osdRowsCur++;
                    j -= 200 / osdRows;
                    if (osdRowsCur > osdRows - 1) {
                        break;
                    }
                }
            }

            if ((osdRowsCur > -1 && osdScroll == -1) || osdRowsCur > osdRows) {
                long j = (getTicks() - osdScrTime);
                while (j > -1) {
                    osdRowsCur--;
                    j -= 200 / osdRows;
                    if (osdRowsCur < 1) {
                        break;
                    }
                }
            }

            // #GDX 15.07.2024 Console deadlock?
            if (osdRowsCur == -1 && osdScroll == -1) {
                osdDraw = false;
            }

            osdScrTime = getTicks();
        }

        if (!osdDraw || osdRowsCur <= 0) {
            return;
        }

        func.clearbg(osdCols, (int) ((osdRowsCur + 1) * osdTextScale / 65536.0f));
        ListNode<OsdString> node = osdHead;
        int row = osdRowsCur - 2;
        for (; node != null && row > 0; node = node.getNext()) {
            row -= func.drawosdstr(0, row, node.get(), osdCols, osdTextShade, osdTextPal, osdTextScale);
        }

        drawPrompt();
    }

    private void drawPrompt() {
        int osdEditShade = prompt.osdEditShade;
        int osdPromptShade = prompt.osdPromptShade;
        OsdColor osdPromptPal = prompt.osdPromptPal;
        OsdColor osdEditPal = prompt.osdEditPal;
        OsdColor osdVersionPal = prompt.osdVersionPal;

        int ypos = osdRowsCur;
        int textScale = osdTextScale;

        int shade = osdPromptShade;
        if (shade == 0) {
            shade = func.getPulseShade(4);
        }

        if (isOnLastLine()) {
            func.drawchar(0, ypos, '~', shade, osdPromptPal, textScale);
        } else if (!isOnFirstLine()) {
            func.drawchar(0, ypos, '^', shade, osdPromptPal, textScale);
        }

        int offset = 0;
        if (prompt.isCapsLockPressed()) {
            if (isOnFirstLine()) {
                offset = 1;
            }
            func.drawchar(offset, ypos, 'C', shade, osdPromptPal, textScale);
        }

        if (prompt.isShiftPressed()) {
            offset = 1;
            if ((prompt.isCapsLockPressed() && isOnFirstLine())) {
                offset = 2;
            }
            func.drawchar(offset, ypos, 'H', shade, osdPromptPal, textScale);
        }

        offset = 0;
        if (prompt.isCapsLockPressed() && prompt.isShiftPressed() && isOnFirstLine()) {
            offset = 1;
        }
        func.drawchar(2 + offset, ypos, '>', shade, osdPromptPal, textScale);

        String inputText = prompt.getTextInput();
        int len = Math.min(osdCols - 1 - 3 - offset, inputText.length());
        for (int x = len - 1; x >= 0; x--) {
            func.drawchar(3 + x + offset, ypos, inputText.charAt(x), osdEditShade << 1, osdEditPal, textScale);
        }

        offset += 3 + prompt.getCursorPosition();

        func.drawcursor(offset, ypos, prompt.isOsdOverType(), textScale);

        String osdVersionText = prompt.osdVersionText;
        if (osdVersionText != null) {
            int xpos = osdCols - osdVersionText.length() + 2;
            func.drawstr(xpos, ypos - ((offset >= xpos) ? 1 : 0),
                    osdVersionText, func.getPulseShade(4), osdVersionPal, textScale);
        }
    }

    public boolean isCaptured() {
        return prompt.isCaptured();
    }

    public void print(String text, OsdColor color) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (color == osdTextPal || color == OsdColor.DEFAULT) {
            System.out.println(text);
        } else {
            System.out.printf("%s%s%s\n", color, text, OsdColor.RESET);
        }

        if (logger != null) {
            logger.write(text);
        }

        OsdString osdString = newLine().get();

        int chp = 0;
        int s = osdTextShade;
        int pal = color.getPal();

        do {
            if (text.charAt(chp) == '\n') {
                osdCharacterPos = 0;
                osdString = newLine().get();
                continue;
            }

            if (text.charAt(chp) == '\r') {
                osdCharacterPos = 0;
                continue;
            }

            if (text.charAt(chp) == '\t') {
                for (int i = 0; i < 2; i++) {
                    osdString.insert(osdCharacterPos++, ' ', pal, s);
                }
                continue;
            }

            if (text.charAt(chp) == '^') {
                StringBuilder number = new StringBuilder();
                int pos = chp + 1;
                if (pos >= text.length()) {
                    continue;
                }

                char num1 = text.charAt(pos++);
                if (Character.isDigit(num1)) {
                    number.append(num1);
                    while(pos < text.length() && Character.isDigit(text.charAt(pos)) && number.length() < 3) {
                        number.append(text.charAt(pos));
                        pos++;
                    }

                    if (number.length() > 0) {
                        pal = Integer.parseInt(number.toString(), 10);
                        chp = pos - 1;
                        continue;
                    }
                } else if (num1 == 'S' || num1 == 's') {
                    chp++;
                    char num = text.charAt(++chp);
                    if (Character.isDigit(num)) {
                        number.append(num);
                        s = Integer.parseInt(number.toString(), 10);
                        continue;
                    }
                } else if (num1 == 'O' || num1 == 'o') {
                    pal = color.getPal();
                    s = osdTextShade;
                    chp++;
                    continue;
                }
            }

            osdString.insert(osdCharacterPos++, text.charAt(chp), pal, s);
        } while (++chp < text.length());
    }

    public void println(String text) {
        print(text, osdTextPal);
        osdCharacterPos = 0;
    }

    public void println(String text, OsdColor color) {
        print(text, color);
        osdCharacterPos = 0;
    }

    private ListNode<OsdString> newLine() {
        ListNode<OsdString> last;
        if (osdTextList.getSize() < MAX_LINES) {
            last = new ListNode<OsdString>(osdTextList.getSize()) {
                final OsdString value = new OsdString();
                @Override
                public OsdString get() {
                    return value;
                }
            };
        } else {
            last = osdTextList.removeLast();
            last.get().clear();
        }
        osdTextList.addFirst(last);
        onPageDown();
        return last;
    }

    void printCommandList(List<String> list, String header, String footer) {
        if (!list.isEmpty()) {
            int maxLength = 0;
            for (String s : list) {
                maxLength = Math.max(maxLength, s.length());
            }
            maxLength += 3;

            println(header, OsdColor.RED);
            StringBuilder msg = new StringBuilder();
            int currentLength = 0;
            for (String s : list) {
                msg.append("  ");
                msg.append(s);
                currentLength += maxLength + 2;
                //FIXME msg.append(" ".repeat(Math.max(0, maxLength - s.length())));

                if (currentLength > osdCols) {
                    msg.append("\n");
                    currentLength = 0;
                }
            }
            println(msg.toString());
            println(footer, OsdColor.RED);
        }
    }

    public void setOsdTextScale(float value) {
        value = Math.max(0.5f, value);
        boolean isFullscreen = osdRowsCur == osdMaxRows;
        osdTextScale = (int) (value * 65536.0f);
        revalidate();
        if (isFullscreen) {
            setFullscreen(true);
        } else {
            stopMoving();
        }
    }

    public void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            osdRowsCur = osdMaxRows;
        } else {
            osdRowsCur = -1;
        }

        osdDraw = fullscreen;
        func.showOsd(fullscreen);
    }

    private void stopMoving() {
        if (osdRowsCur != -1) {
            osdRowsCur = Math.max(-1, osdRows);
        }
    }

    public boolean isOnLastLine() {
        return osdHead == osdTextList.getLast();
    }

    public boolean isOnFirstLine() {
        return osdHead == osdTextList.getFirst();
    }

    public void setFirstLine() {
        osdHead = osdTextList.getFirst();
    }

    public void setLogger(ConsoleLogger logger) {
        this.logger = logger;
    }

    public ConsoleLogger getLogger() {
        return logger;
    }

    private long getTicks() {
        return System.currentTimeMillis();
    }

    // Controller

    public void onToggle() {
        osdScroll = osdDraw ? -1 : 1;
        osdRowsCur += osdScroll;
        prompt.setCaptureInput(!osdDraw);
        func.showOsd(!osdDraw);
        osdScrTime = getTicks();
    }

    void onClose() {
        osdScroll = -1;
        osdRowsCur--;
        prompt.setCaptureInput(false);
        osdScrTime = getTicks();
    }

    void onPageUp() {
        if (!isOnLastLine()) {
            if (osdHead != null && osdHead.getNext() != null) {
                osdHead = osdHead.getNext();
            }
        }
    }

    void onPageDown() {
        if (!isOnFirstLine()) {
            if (osdHead != null && osdHead.getPrev() != null) {
                osdHead = osdHead.getPrev();
            }
        }
    }

    void onFirstPage() {
        osdHead = osdTextList.getLast();
    }

    void onLastPage() {
        osdHead = osdTextList.getFirst();
    }

    void onTabPressed() {
        String input = prompt.getTextInput();
        if (!finder.isListPresent()) {
            List<String> commands = finder.getCommands(input);
            if (commands.size() > 1) {
                printCommandList(commands, String.format("Found %d possible completions for \"%s\"", commands.size(), input), "Press TAB again to cycle through matches");
            } else if (commands.size() == 1) {
                prompt.setTextInput(commands.get(0));
            }
        } else {
            prompt.setTextInput(finder.getNextTabCommand());
        }
    }

    public void onKeyReleased(int keyId) {
        prompt.keyUp(keyId);
    }

    public boolean onKeyPressed(int keyId) {
        if (!isCaptured()) {
            return false;
        }

        if (prompt.keyDown(keyId)) {
            finder.reset();
            return true;
        }

        switch (keyId) {
            case Input.Keys.ESCAPE:
                onClose();
                return true;
            case Input.Keys.TAB:
                onTabPressed();
                return true;
            case Input.Keys.END:
                if (prompt.isCtrlPressed()) {
                    onLastPage();
                    return true;
                }
                break;
            case Input.Keys.HOME:
                if (prompt.isCtrlPressed()) {
                    onFirstPage();
                    return true;
                }
                break;
            case Input.Keys.PAGE_UP:
                onPageUp();
                return true;
            case Input.Keys.PAGE_DOWN:
                onPageDown();
                return true;
        }

        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        if(!isMoving() && prompt.keyTyped(character)) {
            finder.reset();
            return true;
        }
        return false;
    }

//    @Override
//    public boolean keyRepeat(int keycode) {
//        throw new RuntimeException("Console keyRepeat");
////        return keyDown(keycode);
//    }

    @Override
    public boolean keyDown(int keycode) {
        return Console.out.onKeyPressed(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        Console.out.onKeyReleased(keycode);
        return true;
    }

    @Override
    public boolean gameKeyDown(GameKey gameKey) {
        if (GameKeys.Show_Console.equals(gameKey)) {
            onToggle();
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled (float amountX, float amount) {
        if (amount < 0) {
            onPageUp();
            return true;
        } else if (amount > 0) {
            onPageDown();
            return true;
        }
        return false;
    }
}
