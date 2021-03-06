/*
 * Copyright 2015 IBM Corp.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.doctorbatch.magicsauce.websphere.mvs;

import java.util.List;
import java.util.Properties;

import com.ibm.doctorbatch.magicsauce.ClassUtil;
import com.ibm.doctorbatch.magicsauce.mvs.MVSRecordWriter;
import com.ibm.doctorbatch.magicsauce.websphere.etl.Writable;
import com.ibm.etools.marshall.RecordBytes;
import com.ibm.websphere.batch.BatchDataStream;

/**
 * <p>This batch data stream reads records from an MVS data set. See {@link AbstractMVSDataStream} for
 * properties and usage. This BDS reads a single MVS record per invocation of read(), 
 * to customize this behavior, you may either extend AbstractMVSDataStream directly, or 
 * extend this class and override fetchRecord( ZFile, RecordParser ).</p>
 * 
 * @see AbstractMVSDataStream
 * @author Timothy C. Fanelli (tfanelli@us.ibm.com, tim@fanel.li, doc@torbat.ch)
 * @param <T>
 */
public class MVSRecordWriterBDS<T extends RecordBytes> extends AbstractMVSDataStream<T> implements Writable<T>, BatchDataStream {
	private MVSRecordWriter<T> magicSauceWriter;
	private String magicSauceReaderClassName;
	private long recordCount;
	
	@Override
	public void initialize( Properties props ) {
		super.initialize(props);
		
		magicSauceReaderClassName = props.getProperty("magicSauceReaderClassName");
		magicSauceWriter = ClassUtil.getInstanceForClass( magicSauceReaderClassName );
		magicSauceWriter.setDataSetManager( super.getMVSDataSetManager() );
	}
	
	@Override
	public void write(T record) {
		if ( record != null )
		{
			magicSauceWriter.write( record );
			++recordCount;
		}
	}

	/**
	 * Allocates a dynamic DD card using the DD_ALLOC (or DD_ALLOC_RESTART) statement,
	 * and then opens the file using the ds_parameters (or ds_parameters_restart) options, 
	 * provided in the xJCL. 
	 */
	@Override
	public void open() {
		magicSauceWriter.open(null);
	}
	
	@Override
	public void close() {
		magicSauceWriter.close();
	}
	
	@Override
	public String externalizeCheckpointInformation() {
		return Long.toString( recordCount );
	}

	@Override
	public void internalizeCheckpointInformation(String token) {
		long targetRecordCount = Long.parseLong( token );

		super.getMVSDataSetManager().setPosition(targetRecordCount);
		recordCount = targetRecordCount;
	}

	@Override
	public void write(List<? extends T> records) {
		for ( T t : records )
			write(t);
	}

}
