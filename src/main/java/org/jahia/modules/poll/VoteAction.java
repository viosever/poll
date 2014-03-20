/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.poll;

import org.jahia.bin.Action;
import org.jahia.services.content.JCRSessionWrapper;
import org.slf4j.Logger;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.bin.ActionResult;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;

import javax.jcr.NodeIterator;
import javax.jcr.version.VersionManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


/**
 * User: fabrice
 * Date: Apr 15, 2010
 * Time: 10:30:20 AM
 */
public class VoteAction extends Action {
     private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(VoteAction.class);

    // Vote action
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        String answerUUID = req.getParameter("answerUUID");





        // Poll Node management
        // Get the pollNode
        JCRNodeWrapper pollNode = session.getNodeByUUID(renderContext.getMainResource().getNode().getIdentifier());

        // Get a version manager
        VersionManager versionManager = pollNode.getSession().getWorkspace().getVersionManager();

        // Check out poll node
        if(!versionManager.isCheckedOut(pollNode.getPath()))
        	versionManager.checkout(pollNode.getPath());

                        
        // Update total of votes (poll node)
        long totalOfVotes = pollNode.getProperty("totalOfVotes").getLong();
        pollNode.setProperty("totalOfVotes", totalOfVotes+1);



        // Answer node management
        // Get the answer node
        JCRNodeWrapper answerNode = pollNode.getSession().getNodeByUUID(answerUUID);
        // Check out answerNode
        if(!versionManager.isCheckedOut(answerNode.getPath()))
        	versionManager.checkout(answerNode.getPath());
        // Increment nb votes
        long nbOfVotes = answerNode.getProperty("nbOfVotes").getLong();
        answerNode.setProperty("nbOfVotes", nbOfVotes+1);

        // Save
        pollNode.getSession().save();
        // Check in
        versionManager.checkin(pollNode.getPath());
        // Check in poll node
        versionManager.checkin(answerNode.getPath());

        return new ActionResult(HttpServletResponse.SC_OK, null, generateJSONObject(pollNode));
    }

    public static JSONObject generateJSONObject(JCRNodeWrapper pollNode) throws Exception {
        NodeIterator answerNodes = pollNode.getNode("answers").getNodes();

        Map<String, Object> props = new HashMap<String, Object>();
        ArrayList<Map<String, Object>> answersContainer = new ArrayList<Map<String, Object>>();

        long totalVote = pollNode.getProperty("totalOfVotes").getLong();

        // Poll name + total votes
        props.put("totalOfVotes", totalVote);
        props.put("question", pollNode.getProperty("question").getString());

        // Each answer name + total vote
        while (answerNodes.hasNext()) {
            Map<String, Object> answerProperties = new HashMap<String, Object>();
            javax.jcr.Node answer = answerNodes.nextNode();
            long answerVotes = answer.getProperty("nbOfVotes").getLong();

            answerProperties.put("label", answer.getProperty("label").getString());
            answerProperties.put("nbOfVotes", answerVotes);
            answerProperties.put("percentage", (totalVote == 0 || answerVotes == 0)?0:(answerVotes/totalVote)*100);

            answersContainer.add(answerProperties);
        }

        props.put("answerNodes", answersContainer.toArray());

        return new JSONObject(props);
    }
}
