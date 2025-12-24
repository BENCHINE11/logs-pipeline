import streamlit as st
import pandas as pd
import pyarrow.parquet as pq
import glob

st.title("Logs Analytics Dashboard")

base = st.text_input("Chemin local analytics (parquet exporté)", "./analytics_local")

def read_parquet_folder(pattern):
    files = glob.glob(pattern, recursive=True)
    if not files:
        return None
    tables = [pq.read_table(f).to_pandas() for f in files]
    return pd.concat(tables, ignore_index=True)

dt = st.text_input("dt", "2025-12-22")

per_minute = read_parquet_folder(f"{base}/per_minute/dt={dt}/*.parquet")
top_paths = read_parquet_folder(f"{base}/top_paths/dt={dt}/*.parquet")
top_status = read_parquet_folder(f"{base}/top_status/dt={dt}/*.parquet")

if per_minute is not None:
    st.subheader("Hits per minute")
    per_minute["minute"] = pd.to_datetime(per_minute["minute"])
    per_minute = per_minute.sort_values("minute")
    st.line_chart(per_minute.set_index("minute")[["hits"]])

    st.subheader("Avg RT (ms)")
    st.line_chart(per_minute.set_index("minute")[["avg_rt_ms"]])

    st.subheader("5xx error rate")
    st.line_chart(per_minute.set_index("minute")[["error_rate_5xx"]])
else:
    st.warning("per_minute introuvable")

if top_paths is not None:
    st.subheader("Top paths")
    st.dataframe(top_paths.sort_values("hits", ascending=False).head(20))
else:
    st.warning("top_paths introuvable")

if top_status is not None:
    st.subheader("Top status codes")
    st.dataframe(top_status.sort_values("hits", ascending=False))
else:
    st.warning("top_status introuvable")
