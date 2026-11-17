#!/usr/bin/env node
/**
 * The `dsh-acp-tui` executable.
 *
 * A file, not a shell shim on `PATH`: the panel starts this script with Node so
 * the terminal is the root process of the session (a `.bin` shim would put
 * `/bin/sh` there instead, and the panel's PTY would then be attached to a
 * process that outlives the terminal it represents).
 */
import { run } from '../src/main.js'

run()
