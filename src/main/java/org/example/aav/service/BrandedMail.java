package org.example.aav.service;

import org.springframework.web.util.HtmlUtils;

/** Self-contained email layout; no remote fonts, images or application CSS. */
public final class BrandedMail {
  private BrandedMail() {}

  public static String html(String title, String text) {
    var content = new StringBuilder();
    for (String line : text.split("\\R")) {
      if (line.isBlank()) continue;
      String style = line.startsWith("Итого:")
          ? "font-size:22px;font-weight:bold;color:#285b3b;border-top:1px solid #dce5d7;padding-top:18px;"
          : "font-size:16px;line-height:1.7;color:#354738;";
      content.append("<p style=\"").append(style).append("\">")
          .append(HtmlUtils.htmlEscape(line)).append("</p>");
    }
    return """
        <!doctype html><html lang="ru"><head><meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="margin:0;background:#f6f5ee;font-family:Arial,sans-serif;">
        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f6f5ee;">
        <tr><td align="center" style="padding:32px 12px;">
        <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="width:100%%;max-width:600px;">
        <tr><td style="padding:28px;background:#285b3b;color:#fff;border-radius:20px 20px 0 0;">
        <div style="font-size:27px;font-weight:bold;">✳ зелёная лавка.</div>
        <div style="margin-top:10px;font-size:14px;color:#e3edda;">Хорошие продукты. С заботой о вас.</div>
        </td></tr><tr><td style="padding:28px;background:#fff;">
        <h1 style="font-family:Georgia,serif;font-size:28px;color:#285b3b;">%s</h1>
        %s
        <p style="margin-top:32px;color:#354738;line-height:1.7;">С уважением и заботой,<br>
        <strong>команда «Зелёной лавки»</strong></p>
        </td></tr><tr><td style="padding:22px;background:#e9efdf;border-radius:0 0 20px 20px;color:#52634b;font-size:13px;line-height:1.6;">
        Свежесть рядом. Каждый день.<br>Это автоматическое уведомление вашего магазина.
        </td></tr></table></td></tr></table></body></html>
        """.formatted(HtmlUtils.htmlEscape(title), content);
  }

  public static String plain(String body) {
    return body + "\n\nС уважением и заботой,\nкоманда «Зелёной лавки»\nСвежесть рядом. Каждый день.";
  }
}
