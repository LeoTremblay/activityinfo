<#--
 #%L
 ActivityInfo Server
 %%
 Copyright (C) 2009 - 2013 UNICEF
 %%
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as
 published by the Free Software Foundation, either version 3 of the 
 License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public 
 License along with this program.  If not, see
 <http://www.gnu.org/licenses/gpl-3.0.html>.
 #L%
-->
<#-- @ftlvariable name="" type="org.activityinfo.server.mail.Invitation" -->
Bonjour ${newUser.name},

${invitingUser.name} (${invitingUser.email}) vous invite à acceder au ActivityInfo. Pour confirmer
votre adresse et connecter au système, visitez le lien suivante:

https://${domain.host}/confirm?${newUser.changePasswordKey}

Cordialement,

L'équipe ActivityInfo