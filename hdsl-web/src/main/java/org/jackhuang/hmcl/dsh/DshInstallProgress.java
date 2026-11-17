package org.jackhuang.hmcl.dsh;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

/// Reads what a package manager prints into something worth showing.
///
/// The launcher does not download packages itself — npm and pnpm do — so it
/// cannot report bytes fetched. It can report what those programs say they have
/// done, which is where their output comes in.
///
/// Two shapes are read. pnpm prints a running tally:
///
/// ```
/// Progress: resolved 43, reused 30, downloaded 13, added 40
/// ```
///
/// which gives both a sentence and a fraction. npm prints a line per package
/// without ever stating a total, so it gives the sentence and leaves the bar
/// alone.
///
/// The HDSL-web copy is a plain value object: no UI properties, no toolkit
/// thread. A listener registered through [#addListener] is called on
/// the thread that read the line, which is where the web server's event bus
/// forwards it to every connected browser.
@NotNullByDefault
public final class DshInstallProgress {
    /// pnpm's running tally.
    private static final Pattern PNPM = Pattern.compile(
            "Progress: resolved (\\d+), reused (\\d+), downloaded (\\d+), added (\\d+)");

    /// npm's line for a request it answered from its cache.
    private static final Pattern NPM_CACHE = Pattern.compile("npm http cache ");

    /// npm's line for a request it made over the network.
    private static final Pattern NPM_FETCH = Pattern.compile("npm http fetch ");

    /// The line to show, which starts as the launcher's own word for work in
    /// progress and becomes what the package manager reports.
    private String message = i18n("message.doing");

    /// How far along the work is, or -1 while that cannot be said.
    private double fraction = -1;

    /// How many npm requests have been seen, from the cache and over the network.
    private int npmSeen;

    /// How many of those npm answered from its cache.
    private int npmCached;

    /// Who is told when the message or the fraction changes.
    private final List<Consumer<DshInstallProgress>> listeners = new CopyOnWriteArrayList<>();

    /// The line to show.
    ///
    /// @return the line
    public String getMessage() {
        return message;
    }

    /// How far along the work is.
    ///
    /// @return the fraction, which is -1 while it cannot be said
    public double getFraction() {
        return fraction;
    }

    /// Registers a listener called with this object whenever the message or
    /// the fraction changes.
    ///
    /// @param listener the listener
    public void addListener(Consumer<DshInstallProgress> listener) {
        listeners.add(listener);
    }

    /// Reads one line of a package manager's output.
    ///
    /// Called from the thread running the package manager.
    ///
    /// Lines that say nothing about progress are ignored, which is most of them.
    ///
    /// @param line the line
    public void accept(String line) {
        Matcher pnpm = PNPM.matcher(line);
        if (pnpm.find()) {
            int resolved = Integer.parseInt(pnpm.group(1));
            int reused = Integer.parseInt(pnpm.group(2));
            int downloaded = Integer.parseInt(pnpm.group(3));
            int added = Integer.parseInt(pnpm.group(4));

            message = i18n("dsh.install.progress.pnpm", added, resolved, downloaded, reused);
            if (resolved > 0) {
                fraction = Math.min(1.0, (double) added / resolved);
            }
            publish();
            return;
        }

        if (NPM_FETCH.matcher(line).find()) {
            npmSeen++;
        } else if (NPM_CACHE.matcher(line).find()) {
            npmSeen++;
            npmCached++;
        } else {
            return;
        }

        // npm never says how many it will ask for, so there is no fraction to
        // give; the count is still worth reading, because a run served entirely
        // from the cache looks the same as a run that is doing nothing.
        message = i18n("dsh.install.progress.npm", npmSeen, npmSeen - npmCached, npmCached);
        publish();
    }

    /// Tells every listener something changed.
    private void publish() {
        for (Consumer<DshInstallProgress> listener : listeners) {
            listener.accept(this);
        }
    }
}
