/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.mm2d.android.avt.MrControlPoint;
import net.mm2d.android.cds.CdsObject;
import net.mm2d.android.cds.MediaServer;
import net.mm2d.android.cds.Tag;
import net.mm2d.android.util.AribUtils;
import net.mm2d.android.util.LaunchUtils;
import net.mm2d.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CDSアイテムの詳細情報を表示するFragment。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class CdsDetailFragment extends Fragment
        implements PropertyAdapter.OnItemLinkClickListener {
    private static final String TAG = "CdsDetailFragment";

    /**
     * インスタンスを作成する。
     *
     * <p>Bundleの設定と読み出しをこのクラス内で完結させる。
     *
     * @param udn    表示するサーバのUDN
     * @param object 表示するObject
     * @return インスタンス。
     */
    public static CdsDetailFragment newInstance(String udn, CdsObject object) {
        final CdsDetailFragment instance = new CdsDetailFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(Const.EXTRA_SERVER_UDN, udn);
        arguments.putParcelable(Const.EXTRA_OBJECT, object);
        instance.setArguments(arguments);
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.frg_cds_detail, container, false);

        final String udn = getArguments().getString(Const.EXTRA_SERVER_UDN);
        final MediaServer server = DataHolder.getInstance().getMsControlPoint().getDevice(udn);
        final CdsObject object = getArguments().getParcelable(Const.EXTRA_OBJECT);
        if (object == null || server == null) {
            getActivity().finish();
            return rootView;
        }

        final TextView titleView = (TextView) rootView.findViewById(R.id.title);
        if (titleView != null) {
            final String title = object.getTitle();
            titleView.setText(AribUtils.toDisplayableString(title));
            titleView.setBackgroundColor(ThemeUtils.getAccentColor(title));
        }

        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.cds_detail);
        final PropertyAdapter adapter = new PropertyAdapter(getContext());
        adapter.setOnItemLinkClickListener(this);
        setupPropertyAdapter(getActivity(), adapter, object);
        recyclerView.setAdapter(adapter);

        setUpPlayButton(rootView, object);
        setUpSendButton(rootView, udn, object);
        return rootView;
    }

    private void setUpPlayButton(View rootView, final CdsObject object) {
        final FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab_play);
        fab.setVisibility(hasResource(object) ? View.VISIBLE : View.GONE);
        final boolean protectedResource = hasProtectedResource(object);
        final int color = protectedResource ? Color.GRAY : ContextCompat.getColor(getContext(), R.color.accent);
        fab.setBackgroundTintList(ColorStateList.valueOf(color));
        fab.setOnClickListener(view -> {
            if (protectedResource) {
                Snackbar.make(view, R.string.toast_not_support_drm, Snackbar.LENGTH_LONG).show();
            } else {
                ItemSelectHelper.play(getActivity(), object, 0);
            }
        });
        fab.setOnLongClickListener(view -> {
            if (protectedResource) {
                Snackbar.make(view, R.string.toast_not_support_drm, Snackbar.LENGTH_LONG).show();
            } else {
                ItemSelectHelper.play(getActivity(), object);
            }
            return true;
        });
    }

    private void setUpSendButton(View rootView, final String udn, final CdsObject object) {
        final FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab_send);
        final MrControlPoint cp = DataHolder.getInstance().getMrControlPoint();
        if (cp.getDeviceListSize() == 0) {
            fab.setVisibility(View.GONE);
            return;
        }
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(v -> ItemSelectHelper.send(getActivity(), udn, object));
    }

    private static boolean hasResource(CdsObject object) {
        return object.getTagList(CdsObject.RES) != null;
    }

    private static boolean hasProtectedResource(CdsObject object) {
        final List<Tag> tagList = object.getTagList(CdsObject.RES);
        if (tagList == null) {
            return false;
        }
        for (final Tag tag : tagList) {
            final String protocolInfo = tag.getAttribute(CdsObject.PROTOCOL_INFO);
            final String mimeType = CdsObject.extractMimeTypeFromProtocolInfo(protocolInfo);
            if (!TextUtils.isEmpty(mimeType) && mimeType.equals("application/x-dtcp1")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onItemLinkClick(String link) {
        LaunchUtils.openUri(getContext(), link);
    }

    private static String sTb;
    private static String sBs;
    private static String sCs;

    // 初回に変換して保持しておく
    private static void setupString(Context context) {
        if (sTb != null) {
            return;
        }
        sTb = context.getString(R.string.network_tb);
        sBs = context.getString(R.string.network_bs);
        sCs = context.getString(R.string.network_cs);
    }

    static void setupPropertyAdapter(@NonNull Context context,
                                     @NonNull PropertyAdapter adapter,
                                     @NonNull CdsObject object) {
        Log.d(TAG, object.toDumpString());
        setupString(context);
        adapter.addEntry(context.getString(R.string.prop_title),
                AribUtils.toDisplayableString(object.getTitle()));
        adapter.addEntry(context.getString(R.string.prop_channel),
                getChannel(object));
        adapter.addEntry(context.getString(R.string.prop_date),
                getDate(object));
        adapter.addEntry(context.getString(R.string.prop_schedule),
                getSchedule(object));
        adapter.addEntry(context.getString(R.string.prop_genre),
                object.getValue(CdsObject.UPNP_GENRE));

        adapter.addEntry(context.getString(R.string.prop_album),
                object.getValue(CdsObject.UPNP_ALBUM));
        adapter.addEntry(context.getString(R.string.prop_artist),
                jointMembers(object, CdsObject.UPNP_ARTIST));
        adapter.addEntry(context.getString(R.string.prop_actor),
                jointMembers(object, CdsObject.UPNP_ACTOR));
        adapter.addEntry(context.getString(R.string.prop_author),
                jointMembers(object, CdsObject.UPNP_AUTHOR));
        adapter.addEntry(context.getString(R.string.prop_creator),
                object.getValue(CdsObject.DC_CREATOR));

        adapter.addEntry(context.getString(R.string.prop_description),
                jointTagValue(object, CdsObject.DC_DESCRIPTION));
        adapter.addEntryAutoLink(context.getString(R.string.prop_long_description),
                jointLongDescription(object));
        adapter.addEntry(CdsObject.UPNP_CLASS + ":",
                object.getUpnpClass());
    }

    @Nullable
    private static String jointTagValue(@NonNull CdsObject object, String tagName) {
        final List<Tag> tagList = object.getTagList(tagName);
        if (tagList == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (final Tag tag : tagList) {
            if (sb.length() != 0) {
                sb.append('\n');
            }
            sb.append(tag.getValue());
        }
        return AribUtils.toDisplayableString(sb.toString());
    }

    @Nullable
    private static String jointLongDescription(@NonNull CdsObject object) {
        final List<Tag> tagList = object.getTagList(CdsObject.ARIB_LONG_DESCRIPTION);
        if (tagList == null) {
            return null;
        }
        try {
            final StringBuilder sb = new StringBuilder();
            for (final Tag tag : tagList) {
                if (sb.length() != 0) {
                    sb.append('\n');
                    sb.append('\n');
                }
                final String value = tag.getValue();
                if (TextUtils.isEmpty(value)) {
                    continue;
                }
                final byte[] bytes = value.getBytes("UTF-8");
                final int length = Math.min(24, bytes.length);
                final String title = new String(bytes, 0, length, "UTF-8");
                sb.append(title.trim());
                if (value.length() > title.length()) {
                    sb.append('\n');
                    sb.append(value.substring(title.length()));
                }
            }
            return AribUtils.toDisplayableString(sb.toString());
        } catch (final UnsupportedEncodingException ignored) {
        }
        return null;
    }

    @Nullable
    private static String jointMembers(@NonNull CdsObject object, String tagName) {
        final List<Tag> tagList = object.getTagList(tagName);
        if (tagList == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (final Tag tag : tagList) {
            if (sb.length() != 0) {
                sb.append('\n');
            }
            sb.append(tag.getValue());
            final String role = tag.getAttribute("role");
            if (role != null) {
                sb.append(" : ");
                sb.append(role);
            }
        }
        return sb.toString();
    }

    @Nullable
    private static String getChannel(@NonNull CdsObject object) {
        final StringBuilder sb = new StringBuilder();
        final String network = getNetworkString(object);
        if (network != null) {
            sb.append(network);
        }
        final String channelNr = object.getValue(CdsObject.UPNP_CHANNEL_NR);
        if (channelNr != null) {
            if (sb.length() == 0) {
                sb.append(channelNr);
            } else {
                try {
                    final int channel = Integer.parseInt(channelNr);
                    final String nr = String.format(Locale.US, "%1$06d", channel);
                    sb.append(nr.substring(2, 5));
                } catch (final NumberFormatException ignored) {
                }
            }
        }
        final String name = object.getValue(CdsObject.UPNP_CHANNEL_NAME);
        if (name != null) {
            if (sb.length() != 0) {
                sb.append("   ");
            }
            sb.append(name);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    @Nullable
    private static String getNetworkString(@NonNull CdsObject object) {
        final String net = object.getValue(CdsObject.ARIB_OBJECT_TYPE);
        if (net == null) {
            return null;
        }
        switch (net) {
            case "ARIB_TB":
                return sTb;
            case "ARIB_BS":
                return sBs;
            case "ARIB_CS":
                return sCs;
            default:
                return null;
        }
    }

    @Nullable
    private static String getDate(@NonNull CdsObject object) {
        final String str = object.getValue(CdsObject.DC_DATE);
        final Date date = CdsObject.parseDate(str);
        if (date == null) {
            return null;
        }
        if (str.length() <= 10) {
            return DateFormat.format("yyyy/MM/dd (E)", date).toString();
        }
        return DateFormat.format("yyyy/M/d (E) kk:mm:ss", date).toString();
    }

    @Nullable
    private static String getSchedule(@NonNull CdsObject object) {
        final Date start = object.getDateValue(CdsObject.UPNP_SCHEDULED_START_TIME);
        final Date end = object.getDateValue(CdsObject.UPNP_SCHEDULED_END_TIME);
        if (start == null || end == null) {
            return null;
        }
        final String startString = DateFormat.format("yyyy/M/d (E) kk:mm", start).toString();
        final String endString;
        if (end.getTime() - start.getTime() > 12 * 3600 * 1000) {
            endString = DateFormat.format("yyyy/M/d (E) kk:mm", end).toString();
        } else {
            endString = DateFormat.format("kk:mm", end).toString();
        }
        return startString + " ～ " + endString;
    }
}
