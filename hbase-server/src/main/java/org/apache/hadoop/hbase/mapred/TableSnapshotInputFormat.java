package org.apache.hadoop.hbase.mapred;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableSnapshotInputFormatImpl;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class TableSnapshotInputFormat implements InputFormat<ImmutableBytesWritable, Result> {

  static class TableSnapshotRegionSplit implements InputSplit {
    private TableSnapshotInputFormatImpl.InputSplit delegate;

    // constructor for mapreduce framework / Writable
    public TableSnapshotRegionSplit() {
      this.delegate = new TableSnapshotInputFormatImpl.InputSplit();
    }

    public TableSnapshotRegionSplit(TableSnapshotInputFormatImpl.InputSplit delegate) {
      this.delegate = delegate;
    }

    public TableSnapshotRegionSplit(String regionName, List<String> locations) {
      this.delegate = new TableSnapshotInputFormatImpl.InputSplit(regionName, locations);
    }

    @Override
    public long getLength() throws IOException {
      return delegate.getLength();
    }

    @Override
    public String[] getLocations() throws IOException {
      return delegate.getLocations();
    }

    @Override
    public void write(DataOutput out) throws IOException {
      delegate.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
      delegate.readFields(in);
    }
  }

  static class TableSnapshotRecordReader
      implements RecordReader<ImmutableBytesWritable, Result> {

    private TableSnapshotInputFormatImpl.RecordReader delegate;

    public TableSnapshotRecordReader(TableSnapshotRegionSplit split, JobConf job)
        throws IOException {
      delegate = new TableSnapshotInputFormatImpl.RecordReader();
      delegate.initialize(split.delegate, job);
    }

    @Override
    public boolean next(ImmutableBytesWritable key, Result value) throws IOException {
      if (!delegate.nextKeyValue()) {
        return false;
      }
      ImmutableBytesWritable currentKey = delegate.getCurrentKey();
      key.set(currentKey.get(), currentKey.getOffset(), currentKey.getLength());
      value.copyFrom(delegate.getCurrentValue());
      return true;
    }

    @Override
    public ImmutableBytesWritable createKey() {
      return new ImmutableBytesWritable();
    }

    @Override
    public Result createValue() {
      return new Result();
    }

    @Override
    public long getPos() throws IOException {
      return delegate.getPos();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }

    @Override
    public float getProgress() throws IOException {
      return delegate.getProgress();
    }
  }

  @Override
  public InputSplit[] getSplits(JobConf job, int numSplits) throws IOException {
    List<TableSnapshotInputFormatImpl.InputSplit> splits =
      TableSnapshotInputFormatImpl.getSplits(job);
    InputSplit[] results = new InputSplit[splits.size()];
    for (int i = 0; i < splits.size(); i++) {
      results[i] = new TableSnapshotRegionSplit(splits.get(i));
    }
    return results;
  }

  @Override
  public RecordReader<ImmutableBytesWritable, Result>
  getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
    return new TableSnapshotRecordReader((TableSnapshotRegionSplit) split, job);
  }

  /**
   * Configures the job to use TableSnapshotInputFormat to read from a snapshot.
   * @param job the job to configure
   * @param snapshotName the name of the snapshot to read from
   * @param restoreDir a temporary directory to restore the snapshot into. Current user should
   * have write permissions to this directory, and this should not be a subdirectory of rootdir.
   * After the job is finished, restoreDir can be deleted.
   * @throws IOException if an error occurs
   */
  public static void setInput(JobConf job, String snapshotName, Path restoreDir) throws IOException {
    TableSnapshotInputFormatImpl.setInput(job, snapshotName, restoreDir);
  }
}