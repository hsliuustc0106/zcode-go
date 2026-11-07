package com.zcodego.app.remote

/**
 * Builds a one-shot script that types into the element the user already focused.
 * It does not add nodes, replace the page, or press the page's send control.
 */
object FollowUpInsert {
    fun script(text: String): String {
        val literal = jsStringLiteral(text)
        return """
            (function(){
              try {
                var text = $literal;
                var el = document.activeElement;
                if (!el || el === document.body || el === document.documentElement) return 'none';
                var tag = String(el.tagName || '').toLowerCase();
                var type = String(el.type || 'text').toLowerCase();
                var textInput = tag === 'textarea' || (tag === 'input' && /^(text|search|url|email|tel|password)$/.test(type));
                var editable = el.isContentEditable === true;
                if (!textInput && !editable) return 'none';
                if (textInput) {
                  var value = String(el.value || '');
                  var start = el.selectionStart;
                  var end = el.selectionEnd;
                  if (typeof start === 'number' && typeof end === 'number') {
                    el.value = value.slice(0, start) + text + value.slice(end);
                    var pos = start + text.length;
                    try { el.setSelectionRange(pos, pos); } catch (e) {}
                  } else {
                    el.value = value + text;
                  }
                  el.dispatchEvent(new Event('input', { bubbles: true }));
                  el.dispatchEvent(new Event('change', { bubbles: true }));
                  return 'inserted';
                }
                var sel = window.getSelection && window.getSelection();
                if (sel && sel.rangeCount > 0 && el.contains(sel.anchorNode)) {
                  var range = sel.getRangeAt(0);
                  range.deleteContents();
                  range.insertNode(document.createTextNode(text));
                  range.collapse(false);
                } else {
                  el.appendChild(document.createTextNode(text));
                }
                try {
                  el.dispatchEvent(new InputEvent('input', { bubbles: true, data: text, inputType: 'insertText' }));
                } catch (e) {
                  el.dispatchEvent(new Event('input', { bubbles: true }));
                }
                return 'inserted';
              } catch (e) {
                return 'none';
              }
            })();
        """.trimIndent()
    }

    fun interpret(raw: String?): InsertOutcome {
        if (raw.isNullOrBlank() || raw == "null") return InsertOutcome.NO_TARGET
        val cleaned = raw.trim().removeSurrounding("\"").trim()
        return if (cleaned == "inserted") InsertOutcome.INSERTED else InsertOutcome.NO_TARGET
    }

    internal fun jsStringLiteral(value: String): String {
        val out = StringBuilder(value.length + 2)
        out.append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '<' -> out.append("\\u003c")
                '\u2028' -> out.append("\\u2028")
                '\u2029' -> out.append("\\u2029")
                else -> if (ch.code < 0x20) {
                    out.append("\\u")
                    out.append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    out.append(ch)
                }
            }
        }
        out.append('"')
        return out.toString()
    }
}

enum class InsertOutcome {
    INSERTED,
    NO_TARGET,
}
