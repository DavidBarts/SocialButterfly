#!/usr/bin/env python3

# I m p o r t s

import os, sys
import cgi, cgitb
import html
import textwrap
cgitb.enable()

# V a r i a b l e s

STD_HDR = """<!DOCTYPE html>
<html>"""
STD_FTR = "</html>"
PREFIX = "display_"
SUFFIX = ".cgi"

# F u n c t i o n s

def html_head(title):
    print("<head>")
    print('<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />')
    print("<title>" + html.escape(title) + "</title>")
    print(textwrap.dedent("""\
        <script type="text/javascript">
        "use strict";
        function writeToClipboard(elementId) {
            var widget = document.getElementById(elementId);
            widget.select();
            widget.setSelectionRange(0, 99999);
            navigator.clipboard.writeText(widget.value);
        }
        </script>
        """))
    print("</head>")

def preamble(title):
    print(STD_HDR)
    html_head(title)

def postamble():
    print(STD_FTR)

def error(status, brief, verbose):
    print("Status:", status, brief)
    print("")
    preamble("Error")
    print("<h1>Error</h1>")
    print(verbose)
    postamble()
    sys.exit(0)

# M a i n   P r o g r a m

# Initial headers
print("Content-Type: text/html; charset=UTF-8")

# This should not happen
if "REQUEST_METHOD" not in os.environ:
    error("500", "Incomplete environment",
        "<p>The CGI script was invoked with an incomplete set of environment variables.</p>")

# Reject invalid request methods
if os.environ["REQUEST_METHOD"].lower() != "get":
    error("405", "Method not allowed",
        "<p>Only the GET method is allowed for this service.</p>")

# Determine what fields to dump from our name.
myname = os.path.basename(sys.argv[0])
if not(myname.startswith(PREFIX)) or not(myname.endswith(SUFFIX)):
    error("500", "Invalid script name", textwrap.dedent("""\
        <p>CGI script has an invalid name.</p>
        <p>A valid name is of the form <kbd>display_</kbd><i>list</i><kbd>.cgi</kbd>, where <i>list</i> is a comma-separated list of form variables.</p>"""))

# Get form variables of interest
start = len(PREFIX)
end = len(myname) - len(SUFFIX)
form_vars = myname[start:end].split(",")

# Verify all form variables are present
form = cgi.FieldStorage()
for form_var in form_vars:
    if form_var not in form:
        error("400", "Missing form variable",
            f"<p>The <kbd>{form_var}</kbd> form variable is missing.</p>\n" +
             "<p>This can be caused by a refusal to grant the requested " +
             "access, or entering invalid credentials.</p>")

# Looks good, we can issue a successful response
print("Status: 200 OK")
print("")
preamble("OAuth Authentication Data")
print("<h1>OAuth Authentication Data</h1>")
print("<p>OAuth credentials follow. Please enter these into the appropriate configuration fields of the desktop application.</p>")
for form_var in form_vars:
    print(f"<h2>{form_var}</h2>")
    esc = html.escape(form.getfirst(form_var), quote=True)
    print(f'<p><input type="text" value="{esc}" size="80" id="{form_var}" disabled="" />')
    print(f'<button type="button" onclick="writeToClipboard(&quot;{form_var}&quot;)">Copy</button></p>')
postamble()
