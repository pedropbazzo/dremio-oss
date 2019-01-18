/*
 * Copyright (C) 2017-2018 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.store.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dremio.BaseTestQuery;
import com.google.common.io.Resources;

public class TestParquetScan extends BaseTestQuery {

  static FileSystem fs;

  @BeforeClass
  public static void initFs() throws Exception {
    Configuration conf = new Configuration();
    conf.set("fs.default.name", "local");

    fs = FileSystem.get(conf);
  }

  @Test
  public void testSuccessFile() throws Exception {
    Path p = new Path("/tmp/nation_test_parquet_scan");
    if (fs.exists(p)) {
      fs.delete(p, true);
    }

    fs.mkdirs(p);

    byte[] bytes = Resources.toByteArray(Resources.getResource("tpch/nation.parquet"));

    FSDataOutputStream os = fs.create(new Path(p, "nation.parquet"));
    os.write(bytes);
    os.close();
    fs.create(new Path(p, "_SUCCESS")).close();
    fs.create(new Path(p, "_logs")).close();

    testBuilder()
        .sqlQuery("select count(*) c from dfs.tmp.nation_test_parquet_scan where 1 = 1")
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(25L)
        .build()
        .run();
  }

  @Test
  public void testDataPageV2() throws Exception {
    final String sql = "select count(*) as cnt from dfs.\"${WORKING_PATH}/src/test/resources/datapage_v2.parquet\" where extractmonth(Kommtzeit) = 10";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("cnt")
      .baselineValues(570696L)
      .build()
      .run();
  }
}
