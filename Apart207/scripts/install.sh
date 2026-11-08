#!/bin/sh
npm install
python3 -m pip install -r requirements.txt
python3 -c "import markdown, jinja2, yaml, pygments, pymdownx; print('deps verified')"
