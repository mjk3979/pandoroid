/* 
 * Pandoroid - An open source Pandora Internet Radio client for Android.
 * 
 * Copyright (C) 2012  Dylan Powers <dylan.kyle.powers@gmail.com>
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
 * Description: A handy little exception class for stating that THE API IS BROKEN! 
 * 	yup =)
 * 
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class PandoraAPIModifiedException extends PandoraAPIException{

	private static final long serialVersionUID = 7787891447155883573L;

	public PandoraAPIModifiedException(String message){
		super(message);
	}
	
	public PandoraAPIModifiedException(Throwable e){
		super(e);
	}
	
	public PandoraAPIModifiedException(String message, Throwable e){
		super(message, e);
	}

}
