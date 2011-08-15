package net.ion.radon.socketio.server.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.ion.radon.socketio.common.ConnectionState;
import net.ion.radon.socketio.common.DisconnectReason;
import net.ion.radon.socketio.common.SocketIOException;
import net.ion.radon.socketio.server.SocketIOClosedException;
import net.ion.radon.socketio.server.SocketIOFrame;
import net.ion.radon.socketio.server.SocketIOInbound;
import net.ion.radon.socketio.server.SocketIOSession;
import net.ion.radon.socketio.server.Transport;
import net.ion.radon.socketio.server.SocketIOSession.SessionTransportHandler;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.URIUtil;


public abstract class XHRTransport extends AbstractHttpTransport {
	public static final String CONTINUATION_KEY =
		"net.ion.bleujin.socketio.server.transport.XHRTransport.Continuation";
	private final int bufferSize;
	private final int maxIdleTime;

	protected abstract class XHRSessionHelper
			implements SessionTransportHandler, ContinuationListener {
		protected final SocketIOSession session;
		private final TransportBuffer buffer = new TransportBuffer(bufferSize);
		private boolean is_open = false;
		private Continuation continuation = null;
		private final boolean isConnectionPersistant;
		private boolean disconnectWhenEmpty = false;

		XHRSessionHelper(SocketIOSession session, boolean isConnectionPersistant) {
			this.session = session;
			this.isConnectionPersistant = isConnectionPersistant;
			if (isConnectionPersistant) {
				session.setHeartbeat(HEARTBEAT_DELAY);
				session.setTimeout(HEARTBEAT_TIMEOUT);
			} else {
				session.setTimeout((HTTP_REQUEST_TIMEOUT-REQUEST_TIMEOUT)/2);
			}
		}

		protected abstract void startSend(HttpServletResponse response) throws IOException;

		protected abstract void writeData(ServletResponse response, String data) throws IOException;

		protected abstract void finishSend(ServletResponse response) throws IOException;
		
		public void disconnect() {
			session.onDisconnect(DisconnectReason.DISCONNECT);
			abort();
		}

		public void close() {
			session.startClose();
		}

		public ConnectionState getConnectionState() {
			return session.getConnectionState();
		}

		public void sendMessage(SocketIOFrame frame)
				throws SocketIOException {
			if (is_open) {
				System.out.println("Session["+session.getSessionId()+"]: " +
						"sendMessage: [" + frame.getFrameType() + "]: " + frame.getData());
				if (continuation != null && continuation.isInitial()) {
					List<String> messages = buffer.drainMessages();
					messages.add(frame.encode());
					if (messages.size() > 0) {
						StringBuilder data = new StringBuilder();
						for (String msg: messages) {
							data.append(msg);
						}
						try {
							writeData(continuation.getServletResponse(), data.toString());
							session.startHeartbeatTimer();
						} catch (IOException e) {
							throw new SocketIOException(e);
						}
					}
				} else {
					String data = frame.encode();
					if (continuation != null && continuation.isSuspended() &&
							buffer.getAvailableBytes() < data.length()) {
						continuation.resume();
					}
					if (buffer.putMessage(data, maxIdleTime) == false) {
						session.onDisconnect(DisconnectReason.TIMEOUT);
						abort();
						throw new SocketIOException();
					}
					if (continuation != null && continuation.isSuspended()) {
						continuation.resume();
					}
				}
			} else {
				throw new SocketIOClosedException();
			}
		}

		public void sendMessage(String message) throws SocketIOException {
			sendMessage(SocketIOFrame.TEXT_MESSAGE_TYPE, message);
		}

		public void sendMessage(int messageType, String message)
				throws SocketIOException {
			if (is_open && session.getConnectionState() == ConnectionState.CONNECTED) {
				sendMessage(new SocketIOFrame(SocketIOFrame.FrameType.DATA, messageType, message));
			} else {
				throw new SocketIOClosedException();
			}
		}

		public void handle(HttpServletRequest request,
				HttpServletResponse response, SocketIOSession session)
				throws IOException {
			if ("GET".equals(request.getMethod())) {
				if (!is_open && buffer.isEmpty()) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
				} else {
					Continuation cont = (Continuation)request.getAttribute(CONTINUATION_KEY);
					if (continuation != null) {
						if (cont != continuation) {
							if (cont != null) {
								/*
								 * If the request continuation is non-null and doesn't match the
								 * active continuation then it's likely this is a result of an old
								 * continuation being resumes one last time.
								 * Just return and the continuation will be no more.
								 */
								return;
							} else {
								/*
								 * If the request has no continuation but there is an active
								 * continuation for the session then this request is probably due
								 * to the client making spurious requests.
								 * Return with no results.
								 */
								return;
							}
						}
						List<String> messages = buffer.drainMessages();
						if (messages.size() > 0) {
							StringBuilder data = new StringBuilder();
							for (String msg: messages) {
								data.append(msg);
							}
							writeData(continuation.getServletResponse(), data.toString());
							if (isConnectionPersistant) {
								if (!disconnectWhenEmpty) {
									continuation.suspend(response);
								}
							} else {
								finishSend(response);
								request.removeAttribute(CONTINUATION_KEY);
							}
							session.startHeartbeatTimer();
						} else {
							if (!disconnectWhenEmpty) {
								continuation.suspend(response);
							}
						}
					} else if (!isConnectionPersistant) {
						if (cont != null) {
							/*
							 * If the request continuation is not-null and and there is no
							 * active continuation then it's likely this is a result of an old
							 * continuation being resumes one last time.
							 * Just return and the continuation will be no more.
							 */
							return;
						}
						if (!buffer.isEmpty()) {
							List<String> messages = buffer.drainMessages();
							if (messages.size() > 0) {
								StringBuilder data = new StringBuilder();
								for (String msg: messages) {
									data.append(msg);
								}
								startSend(response);
								writeData(response, data.toString());
								finishSend(response);
								if (!disconnectWhenEmpty) {
									session.startTimeoutTimer();
								} else {
									abort();
								}
							}
						} else {
							session.clearTimeoutTimer();
							request.setAttribute(SESSION_KEY, session);
							response.setBufferSize(bufferSize);
							continuation = ContinuationSupport.getContinuation(request);
							continuation.addContinuationListener(this);
							continuation.setTimeout(REQUEST_TIMEOUT);
							continuation.suspend(response);
							request.setAttribute(CONTINUATION_KEY, continuation);
							startSend(response);
						}
					} else {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
					}
				}
			} else if ("POST".equals(request.getMethod())) {
				if (is_open) {
					int size = request.getContentLength();
					BufferedReader reader = request.getReader();
					if (size == 0) {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					} else {
						String data = decodePostData(request.getContentType(), IO.toString(reader));
						if (data != null && data.length() > 0) {
							List<SocketIOFrame> list = SocketIOFrame.parse(data);
							for (SocketIOFrame msg: list) {
								session.onMessage(msg);
							}
						}
					}
				}
			} else {
	    		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
			
		}

		protected String decodePostData(String contentType, String data) {
			if (contentType.startsWith("application/x-www-form-urlencoded")) {
				if (data.substring(0, 5).equals("data=")) {
					return URIUtil.decodePath(data.substring(5));
				} else {
					return "";
				}
			} else if (contentType.startsWith("text/plain")) {
				return data;
			} else {
				// TODO: Treat as text for now, maybe error in the future.
				return data;
			}
		}
		
		public void onComplete(Continuation cont) {
			if (continuation != null && cont == continuation) {
				if (isConnectionPersistant) {
					is_open = false;
					if (!disconnectWhenEmpty) {
						session.onDisconnect(DisconnectReason.DISCONNECT);
					}
					continuation = null;
					abort();
				} else {
					continuation = null;
					if (!is_open && buffer.isEmpty() && !disconnectWhenEmpty) {
						session.onDisconnect(DisconnectReason.DISCONNECT);
						abort();
					}
					if (disconnectWhenEmpty) {
						abort();
					} else {
						session.startTimeoutTimer();
					}
				}
			}
		}

		public void onTimeout(Continuation cont) {
			if (continuation != null && cont == continuation) {
				if (isConnectionPersistant) {
					is_open = false;
					session.onDisconnect(DisconnectReason.TIMEOUT);
					continuation = null;
					abort();
				} else {
					continuation = null;
					if (!is_open && buffer.isEmpty()) {
						session.onDisconnect(DisconnectReason.DISCONNECT);
						abort();
					} else {
						try {
							finishSend(cont.getServletResponse());
						} catch (IOException e) {
							continuation = null;
							session.onDisconnect(DisconnectReason.DISCONNECT);
							abort();
						}
					}
					session.startTimeoutTimer();
				}
			}
		}

		protected abstract void customConnect(HttpServletRequest request,
				HttpServletResponse response) throws IOException;
		
		public void connect(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			request.setAttribute(SESSION_KEY, session);
			response.setBufferSize(bufferSize);
			continuation = ContinuationSupport.getContinuation(request);
			continuation.addContinuationListener(this);
			if (isConnectionPersistant) {
				continuation.setTimeout(HTTP_REQUEST_TIMEOUT*2);
			}
			customConnect(request, response);
			is_open = true;
			session.onConnect(this);
			finishSend(response);
			if (isConnectionPersistant && continuation != null) {
				request.setAttribute(CONTINUATION_KEY, continuation);
				continuation.suspend(response);
			}
		}

		public void disconnectWhenEmpty() {
			disconnectWhenEmpty = true;
		}
		
		public void abort() {
			session.clearHeartbeatTimer();
			session.clearTimeoutTimer();
			is_open = false;
			if (continuation != null) {
				if (continuation.isSuspended()) {
					continuation.complete();
				}
				continuation = null;
			}
			buffer.setListener(new TransportBuffer.BufferListener() {
				public boolean onMessages(List<String> messages) {
					return false;
				}
				
				public boolean onMessage(String message) {
					return false;
				}
			});
			buffer.clear();
			session.onShutdown();
		}
	}

	public XHRTransport(int bufferSize, int maxIdleTime) {
		this.bufferSize = bufferSize;
		this.maxIdleTime = maxIdleTime;
	}

	/**
	 * This method should only be called within the context of an active HTTP request.
	 */
	protected abstract XHRSessionHelper createHelper(SocketIOSession session);
	
	protected SocketIOSession connect(HttpServletRequest request,
			HttpServletResponse response, Transport.InboundFactory inboundFactory,
			net.ion.radon.socketio.server.SocketIOSession.Factory sessionFactory)
			throws IOException {
		SocketIOInbound inbound = inboundFactory.getInbound(request, null);
		if (inbound != null) {
 			SocketIOSession session = sessionFactory.createSession(inbound);
			XHRSessionHelper handler =  createHelper(session);
			handler.connect(request, response);
			return session;
		}
		return null;
	}

}
