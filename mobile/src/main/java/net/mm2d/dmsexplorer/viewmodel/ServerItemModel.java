/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.viewmodel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;

import net.mm2d.android.upnp.cds.MediaServer;
import net.mm2d.android.util.AribUtils;
import net.mm2d.android.util.DrawableUtils;
import net.mm2d.dmsexplorer.R;
import net.mm2d.dmsexplorer.util.ThemeUtils;
import net.mm2d.dmsexplorer.util.ToolbarThemeUtils;
import net.mm2d.upnp.Icon;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class ServerItemModel {
    public final boolean selected;
    public final Drawable accentBackground;
    public final String accentText;
    public final Bitmap accentIcon;
    public final String title;
    public final String description;

    public ServerItemModel(Context context, MediaServer server, boolean selected) {
        this.selected = selected;
        final String name = server.getFriendlyName();
        final Icon icon = server.getIcon();
        title = name;
        description = makeDescription(server);
        if (icon == null) {
            accentIcon = null;
            accentText = TextUtils.isEmpty(name) ? ""
                    : AribUtils.toDisplayableString(name.substring(0, 1));
            accentBackground = DrawableUtils.get(context, R.drawable.ic_circle);
            DrawableCompat.setTint(accentBackground, ThemeUtils.getAccentColor(name));
            ToolbarThemeUtils.setServerThemeColorAsync(server, null);
            return;
        }
        final byte[] binary = icon.getBinary();
        accentIcon = BitmapFactory.decodeByteArray(binary, 0, binary.length);
        accentText = null;
        accentBackground = null;
        ToolbarThemeUtils.setServerThemeColorAsync(server, accentIcon);
    }

    private String makeDescription(MediaServer server) {
        final StringBuilder sb = new StringBuilder();
        sb.append("IP: ");
        sb.append(server.getIpAddress());
        final String serial = server.getSerialNumber();
        if (serial != null && !serial.isEmpty()) {
            sb.append("  ");
            sb.append("Serial: ");
            sb.append(serial);
        }
        return sb.toString();
    }
}
