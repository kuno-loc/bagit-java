package gov.loc.repository.transfer.ui.controllers;

import gov.loc.repository.transfer.ui.UIConstants;
import gov.loc.repository.transfer.ui.dao.WorkflowDao;
import gov.loc.repository.transfer.ui.model.NodeBean;
import gov.loc.repository.transfer.ui.model.TokenBean;
import gov.loc.repository.transfer.ui.model.WorkflowBeanFactory;
import gov.loc.repository.transfer.ui.springframework.ModelAndView;
import gov.loc.repository.transfer.ui.utilities.PermissionsHelper;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TokenController extends AbstractRestController {

	public static final String TOKENID = "tokenId";

	@Override
	public String getUrlParameterDescription() {
		return "token/{tokenId}\\.{format}";
	}

	@RequestMapping
	@Override
	public ModelAndView handleRequest(
			HttpServletRequest request, 
			HttpServletResponse response) throws Exception 
	{
		return this.handleRequestInternal(request, response);
	}
	
	@Override
	protected void handlePut(HttpServletRequest request, ModelAndView mav,
			WorkflowBeanFactory factory, WorkflowDao dao,
			PermissionsHelper permissionsHelper, Map<String, String> urlParameterMap) throws Exception {

		if (! urlParameterMap.containsKey(TOKENID)) {
			mav.setError(HttpServletResponse.SC_BAD_REQUEST, "Token id not provided");
			return;
		}
		
		String tokenId = urlParameterMap.get(TOKENID);
		TokenBean tokenBean = dao.getTokenBean(tokenId);
		if (tokenBean == null)
		{
			mav.setError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (! tokenBean.isMovable())
		{
			mav.setError(HttpServletResponse.SC_BAD_REQUEST, "Token id not movable");
			return;			
		}
		
		String nodeName = request.getParameter(UIConstants.PARAMETER_NODE);
		if (nodeName == null)
		{
			mav.setError(HttpServletResponse.SC_BAD_REQUEST, "Node not provided");
			return;						
		}
		NodeBean nodeBean = tokenBean.getProcessInstanceBean().getProcessDefinitionBean().getNodeBean(nodeName);
		if (nodeBean == null)
		{
			mav.setError(HttpServletResponse.SC_BAD_REQUEST, "Node not recognized");
			return;									
		}
		tokenBean.setNodeBean(nodeBean);
		dao.save(tokenBean);
		request.getSession().setAttribute(UIConstants.SESSION_MESSAGE, "The token was moved.");
		
		mav.setViewName("redirect:/processinstance/" + tokenBean.getProcessInstanceBean().getId() + ".html");
		
	}	
	
}
