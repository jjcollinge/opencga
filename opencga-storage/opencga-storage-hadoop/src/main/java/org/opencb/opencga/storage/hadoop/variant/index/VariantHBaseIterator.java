package org.opencb.opencga.storage.hadoop.variant.index;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.opencga.storage.core.variant.StudyConfigurationManager;
import org.opencb.opencga.storage.core.variant.adaptors.VariantDBIterator;
import org.opencb.opencga.storage.hadoop.variant.GenomeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created on 23/11/15
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class VariantHBaseIterator extends VariantDBIterator {

    private final Logger logger = LoggerFactory.getLogger(VariantHBaseIterator.class);
    private final ResultScanner resultScanner;
    private final GenomeHelper genomeHelper;
    private final Iterator<Result> iterator;
    private final HBaseVariantToVariantConverter converter;
    private long limit = Long.MAX_VALUE;
    private long count = 0;

    public VariantHBaseIterator(ResultScanner resultScanner, VariantTableHelper variantTableHelper, QueryOptions options) throws IOException {
        this.resultScanner = resultScanner;
        this.genomeHelper = variantTableHelper;
        iterator = resultScanner.iterator();
        converter = new HBaseVariantToVariantConverter(variantTableHelper);
        setLimit(options.getLong("limit"));
    }

    public VariantHBaseIterator(ResultScanner resultScanner, GenomeHelper genomeHelper, StudyConfigurationManager scm, QueryOptions options)
            throws IOException {
        this.resultScanner = resultScanner;
        this.genomeHelper = genomeHelper;
        iterator = resultScanner.iterator();
        converter = new HBaseVariantToVariantConverter(genomeHelper, scm);
        setLimit(options.getLong("limit"));
    }

    @Override
    public boolean hasNext() {
        return count < limit && iterator.hasNext();
    }

    @Override
    public Variant next() {
        if (count >= limit) {
            throw new NoSuchElementException("Limit reached");
        }
        count++;
        Result next = fetch(iterator::next);
        return convert(() -> converter.convert(next));
    }

    @Override
    public void close() {
        logger.debug("Close variant iterator. Fetch = {}ms, Convert = {}ms", getTimeFetching() / 100000.0, getTimeConverting() / 1000000.0);
        resultScanner.close();
    }

    public long getLimit() {
        return limit;
    }

    protected void setLimit(long limit) {
        this.limit = limit <= 0 ? Long.MAX_VALUE : limit;
    }
}