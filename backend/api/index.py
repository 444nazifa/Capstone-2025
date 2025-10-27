#!/usr/bin/env python3

from prescription_api import app
import sys
import os

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))


if __name__ == "__main__":
    app.run()
else:
    # This is the WSGI application that Vercel will use
    application = app
