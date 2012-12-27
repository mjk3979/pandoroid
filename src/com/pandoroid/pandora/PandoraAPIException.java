/* This file is part of Pandoroid
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.pandoroid.pandora;

/**
 * Description: An exception class that picks up errors to anything 
 * 	related to the API's presented by Pandora.
 * 
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class PandoraAPIException extends Exception{

	private static final long serialVersionUID = -6889498763596534578L;
	
	public PandoraAPIException(String message){
		super(message);
	}
	
	public PandoraAPIException(Throwable e){
		super(e);
	}
	
	public PandoraAPIException(String message, Throwable e){
		super(message, e);
	}
}
