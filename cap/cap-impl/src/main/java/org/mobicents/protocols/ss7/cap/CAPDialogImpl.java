/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.protocols.ss7.cap;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.mobicents.protocols.asn.AsnOutputStream;
import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.cap.api.CAPDialog;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPServiceBase;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPGprsReferenceNumber;
import org.mobicents.protocols.ss7.cap.api.dialog.CAPUserAbortReason;
import org.mobicents.protocols.ss7.cap.api.errors.CAPErrorMessage;
import org.mobicents.protocols.ss7.cap.errors.CAPErrorMessageImpl;
import org.mobicents.protocols.ss7.tcap.api.TCAPException;
import org.mobicents.protocols.ss7.tcap.api.TCAPSendException;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.Dialog;
import org.mobicents.protocols.ss7.tcap.asn.TcapFactory;
import org.mobicents.protocols.ss7.tcap.asn.comp.ErrorCode;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.mobicents.protocols.ss7.tcap.asn.comp.Parameter;
import org.mobicents.protocols.ss7.tcap.asn.comp.Problem;
import org.mobicents.protocols.ss7.tcap.asn.comp.Reject;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnError;
import org.mobicents.protocols.ss7.tcap.asn.comp.ReturnResultLast;

/**
 * 
 * @author sergey vetyutnev
 */
public abstract class CAPDialogImpl implements CAPDialog {

	private static final Logger logger = Logger.getLogger(CAPDialogImpl.class);

	private Object userObject;

	protected Dialog tcapDialog = null;
	protected CAPProviderImpl capProviderImpl = null;
	protected CAPServiceBase capService = null;

	// Application Context of this Dialog
	protected CAPApplicationContext appCntx;

	protected CAPGprsReferenceNumber gprsReferenceNumber = null;

	protected CAPDialogState state = CAPDialogState.Idle;
	
	protected boolean normalDialogShutDown = false;
	
	private Set<Long> incomingInvokeList = new HashSet<Long>();
	

	protected CAPDialogImpl(CAPApplicationContext appCntx, Dialog tcapDialog, CAPProviderImpl capProviderImpl, CAPServiceBase capService) {
		this.appCntx = appCntx;
		this.tcapDialog = tcapDialog;
		this.capProviderImpl = capProviderImpl;
		this.capService = capService;
	}
	
	@Override
	public void keepAlive() {
		this.tcapDialog.keepAlive();
	}

	public Long getDialogId() {
		return tcapDialog.getDialogId();
	}

	public CAPServiceBase getService() {
		return this.capService;
	}

	public Dialog getTcapDialog() {
		return tcapDialog;
	}

	public void release() {
		this.setNormalDialogShutDown();
		this.setState(CAPDialogState.Expunged);
		
		if (this.tcapDialog != null)
			this.tcapDialog.release();
	}

	/**
	 * Setting that the CAP Dialog is normally shutting down - 
	 * to prevent performing onDialogReleased()  
	 */
	protected void setNormalDialogShutDown() {
		this.normalDialogShutDown = true;
	}
	
	protected Boolean getNormalDialogShutDown() {
		return this.normalDialogShutDown;
	}
	
	/**
	 * Adding the new incoming invokeId into incomingInvokeList list
	 * 
	 * @param invokeId
	 * @return false: failure - this invokeId already present in the list
	 */
	public boolean addIncomingInvokeId(Long invokeId) {
		synchronized (this.incomingInvokeList) {
			if (this.incomingInvokeList.contains(invokeId))
				return false;
			else {
				this.incomingInvokeList.add(invokeId);
				return true;
			}
		}
	}

	public void removeIncomingInvokeId(Long invokeId) {
		synchronized (this.incomingInvokeList) {
			this.incomingInvokeList.remove(invokeId);
		}
	}

	public Boolean checkIncomingInvokeIdExists(Long invokeId) {
		synchronized (this.incomingInvokeList) {
			return this.incomingInvokeList.contains(invokeId);
		}
	}

	public CAPDialogState getState() {
		return state;
	}

	protected synchronized void setState(CAPDialogState newState) {
		if (this.state == CAPDialogState.Expunged) {
			return;
		}
		
		this.state = newState;
		if (newState == CAPDialogState.Expunged) {
			this.capProviderImpl.removeDialog(tcapDialog.getDialogId());
			this.capProviderImpl.deliverDialogResease(this);
		}
	}

	@Override
	public void setGprsReferenceNumber(CAPGprsReferenceNumber gprsReferenceNumber) {
		this.gprsReferenceNumber = gprsReferenceNumber;
	}

	
	@Override
	public void send() throws CAPException {
		// TODO Auto-generated method stub
		// ...........................
	}

	@Override
	public void close(boolean prearrangedEnd) throws CAPException {
		// TODO Auto-generated method stub
		// ...........................
	}

	@Override
	public void abort(CAPUserAbortReason abortReason) throws CAPException {
		// TODO Auto-generated method stub
		// ...........................
	}

	@Override
	public void sendInvokeComponent(Invoke invoke) throws CAPException {

		try {
			this.tcapDialog.sendComponent(invoke);
		} catch (TCAPSendException e) {
			throw new CAPException(e.getMessage(), e);
		}
	}

	@Override
	public void sendReturnResultLastComponent(ReturnResultLast returnResultLast) throws CAPException {

		this.removeIncomingInvokeId(returnResultLast.getInvokeId());

		try {
			this.tcapDialog.sendComponent(returnResultLast);
		} catch (TCAPSendException e) {
			throw new CAPException(e.getMessage(), e);
		}
	}

	@Override
	public void sendErrorComponent(Long invokeId, CAPErrorMessage mem) throws CAPException {
		
		CAPErrorMessageImpl capErrorMessage = (CAPErrorMessageImpl)mem;
		
		this.removeIncomingInvokeId(invokeId);
		
		ReturnError returnError = this.capProviderImpl.getTCAPProvider().getComponentPrimitiveFactory().createTCReturnErrorRequest();

		try {
			returnError.setInvokeId(invokeId);

			// Error Code
			ErrorCode ec = TcapFactory.createErrorCode();
			ec.setLocalErrorCode(capErrorMessage.getErrorCode());
			returnError.setErrorCode(ec);
			
			AsnOutputStream aos = new AsnOutputStream();
			capErrorMessage.encodeData(aos);
			byte[] buf = aos.toByteArray();
			if (buf.length != 0) {
				Parameter p = this.capProviderImpl.getTCAPProvider().getComponentPrimitiveFactory().createParameter();
				p.setTagClass(capErrorMessage.getTagClass());
				p.setPrimitive(capErrorMessage.getIsPrimitive());
				p.setTag(capErrorMessage.getTag());
				p.setData(buf);
				returnError.setParameter(p);
			}

			this.tcapDialog.sendComponent(returnError);

		} catch (TCAPSendException e) {
			throw new CAPException(e.getMessage(), e);
		}
	}

	@Override
	public void sendRejectComponent(Long invokeId, Problem problem) throws CAPException {

		if (invokeId != null && problem != null && problem.getInvokeProblemType() != null)
			this.removeIncomingInvokeId(invokeId);

		Reject reject = this.capProviderImpl.getTCAPProvider().getComponentPrimitiveFactory().createTCRejectRequest();

		try {
			reject.setInvokeId(invokeId);

			// Error Code
			reject.setProblem(problem);

			this.tcapDialog.sendComponent(reject);

		} catch (TCAPSendException e) {
			throw new CAPException(e.getMessage(), e);
		}
	}

	@Override
	public void resetInvokeTimer(Long invokeId) throws CAPException {
		try {
			this.getTcapDialog().resetTimer(invokeId);
		} catch( TCAPException e ) {
			throw new CAPException( "TCAPException occure: " + e.getMessage(), e );
		}
	}

	@Override
	public boolean cancelInvocation(Long invokeId) throws CAPException {
		try {
			return this.getTcapDialog().cancelInvocation(invokeId);
		} catch( TCAPException e ) {
			throw new CAPException( "TCAPException occure: " + e.getMessage(), e );
		}
	}

	@Override
	public Object getUserObject() {
		return this.userObject;
	}

	@Override
	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	@Override
	public CAPApplicationContext getApplicationContext() {
		return appCntx;
	}

	@Override
	public int getMaxUserDataLength() {
		return this.getTcapDialog().getMaxUserDataLength();
	}

	@Override
	public int getMessageUserDataLengthOnSend() throws CAPException {

		// ....................................
//		try {
//			switch (this.tcapDialog.getState()) {
//			case Idle:
//				ApplicationContextName acn = this.capProviderImpl.getTCAPProvider().getDialogPrimitiveFactory()
//						.createApplicationContextName(this.appCntx.getOID());
//
//				TCBeginRequest tb = this.capProviderImpl.encodeTCBegin(this.getTcapDialog(), acn, destReference, origReference, this.extContainer);
//				return tcapDialog.getDataLength(tb);
//
//			case Active:
//				// Its Active send TC-CONTINUE
//
//				TCContinueRequest tc = this.capProviderImpl.encodeTCContinue(this.getTcapDialog(), false, null, null);
//				return tcapDialog.getDataLength(tc);
//
//			case InitialReceived:
//				// Its first Reply to TC-Begin
//
//				ApplicationContextName acn1 = this.capProviderImpl.getTCAPProvider().getDialogPrimitiveFactory()
//						.createApplicationContextName(this.appCntx.getOID());
//
//				tc = this.capProviderImpl.encodeTCContinue(this.getTcapDialog(), true, acn1, this.extContainer);
//				return tcapDialog.getDataLength(tc);
//			}
//		} catch (TCAPSendException e) {
//			throw new CAPException("TCAPSendException when getMessageUserDataLengthOnSend", e);
//		}
		// ....................................

		throw new CAPException("Bad TCAP Dialog state: " + this.tcapDialog.getState());
	}

	@Override
	public int getMessageUserDataLengthOnClose(boolean prearrangedEnd) throws CAPException {

		// ....................................
//		try {
//			switch (this.tcapDialog.getState()) {
//			case InitialReceived:
//				ApplicationContextName acn = this.capProviderImpl.getTCAPProvider().getDialogPrimitiveFactory()
//						.createApplicationContextName(this.appCntx.getOID());
//
//				TCEndRequest te = this.capProviderImpl.encodeTCEnd(this.getTcapDialog(), true, prearrangedEnd, acn, this.extContainer);
//				return tcapDialog.getDataLength(te);
//
//			case Active:
//				te = this.capProviderImpl.encodeTCEnd(this.getTcapDialog(), false, prearrangedEnd, null, null);
//				return tcapDialog.getDataLength(te);
//			}
//		} catch (TCAPSendException e) {
//			throw new CAPException("TCAPSendException when getMessageUserDataLengthOnSend", e);
//		}
		// ....................................

		throw new CAPException("Bad TCAP Dialog state: " + this.tcapDialog.getState());
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("CAPDialog: DialogId=").append(this.getDialogId()).append("CAPDialogState=").append(this.getState())
				.append("CAPApplicationContext=").append(this.appCntx).append("TCAPDialogState=")
				.append(this.tcapDialog.getState());
		return sb.toString();
	}

}