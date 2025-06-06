#ifndef PWR_COMPUTEENGINEPOWER_INCLUDED
#define PWR_COMPUTEENGINEPOWER_INCLUDED
/**
 * @file pwr_computeEnginePower.h
 * Definition of global interface of module pwr_computeEnginePower.c
 *
 * Copyright (C) 2015 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Include files
 */

#include "types.h"


/*
 * Defines
 */


/*
 * Global type definitions
 */


/*
 * Global data declarations
 */


/*
 * Global prototypes
 */

/* The step function of the APSW. */
void pwr_computeEnginePwr();

/** Check the current speed of rotation and engine power against the user set limits. */
void pwr_checkUserLimits();

#endif  /* PWR_COMPUTEENGINEPOWER_INCLUDED */
